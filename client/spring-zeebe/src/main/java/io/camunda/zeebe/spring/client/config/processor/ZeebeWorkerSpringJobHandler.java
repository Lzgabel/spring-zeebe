package io.camunda.zeebe.spring.client.config.processor;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.spring.client.annotation.ZeebeVariable;
import io.camunda.zeebe.spring.client.bean.ParameterInfo;
import io.camunda.zeebe.spring.client.bean.value.ZeebeWorkerValue;
import io.camunda.zeebe.spring.client.exception.ZeebeBpmnError;

import java.util.ArrayList;
import java.util.List;

public class ZeebeWorkerSpringJobHandler implements JobHandler {

  private ZeebeWorkerValue workerValue;
  private CommandHandlingStrategy commandHandlingStrategy;

  public ZeebeWorkerSpringJobHandler(ZeebeWorkerValue workerValue, CommandHandlingStrategy commandHandlingStrategy) {
    this.workerValue = workerValue;
    this.commandHandlingStrategy = commandHandlingStrategy;
  }

  @Override
  public void handle(JobClient jobClient, ActivatedJob job) throws Exception {
    // TODO: Figuring out parameters and assignments could probably also done only once in the beginning to save some computing time on each invocation
    List<Object> args = createParameters(jobClient, job, workerValue.getBeanInfo().getParameters());

    try {
      Object result = workerValue.getBeanInfo().invoke(args.toArray());
      // normal exceptions are handled by JobRunnableFactory
      // (https://github.com/camunda-cloud/zeebe/blob/develop/clients/java/src/main/java/io/camunda/zeebe/client/impl/worker/JobRunnableFactory.java#L45)
      // which leads to retrying
      if (workerValue.isAutoComplete()) {
        commandHandlingStrategy.handleCommandSuccess(jobClient, job, result);
      }
    } catch (ZeebeBpmnError bpmnError) {
      commandHandlingStrategy.handleCommandError(jobClient, job, bpmnError);
    }
  }

  private List<Object> createParameters(JobClient jobClient, ActivatedJob job, List<ParameterInfo> parameters) {
    List<Object> args = new ArrayList<>();
    for (ParameterInfo param : parameters) {
      // parameter default null
      Object arg = null;
      Class<?> clazz = param.getParameterInfo().getType();

      if (JobClient.class.isAssignableFrom(clazz)) {
        arg = jobClient;
      } else if (ActivatedJob.class.isAssignableFrom(clazz)) {
        arg = job;
      } else if (param.getParameterInfo().isAnnotationPresent(ZeebeVariable.class)) {
        try {
          arg = clazz.cast(job.getVariablesAsMap().get(param.getParameterName()));
        }
        catch (ClassCastException ex) {
          throw new RuntimeException("Cannot assign process variable '" + param.getParameterName() + "' to parameter, invalid type found: " + ex.getMessage());
        }
      }
      args.add(arg);
    }
    return args;
  }


}
