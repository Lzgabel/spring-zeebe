package io.camunda.zeebe.spring.client.config.processor;

import io.camunda.zeebe.client.api.command.CompleteJobCommandStep1;
import io.camunda.zeebe.client.api.command.FinalCommandStep;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.exception.ZeebeBpmnError;

import java.io.InputStream;
import java.util.Map;

public class DefaultCommandHandlingStrategy implements CommandHandlingStrategy {


  @Override
  public void handleCommandSuccess(JobClient jobClient, ActivatedJob job, Object result) {
    CompleteJobCommandStep1 completeCommand = jobClient.newCompleteCommand(job);
    if (result != null) {
      if (result.getClass().isAssignableFrom(Map.class)) {
        completeCommand = completeCommand.variables((Map) result);
      } else if (result.getClass().isAssignableFrom(String.class)) {
        completeCommand = completeCommand.variables((String) result);
      } else if (result.getClass().isAssignableFrom(InputStream.class)) {
        completeCommand = completeCommand.variables((InputStream) result);
      } else {
        completeCommand = completeCommand.variables(result);
      }
    }
    FinalCommandStep commandStep = completeCommand;
    completeCommand.send().exceptionally(e -> {
      this.handleCommandError(jobClient, job, commandStep, e);
      return null;
    });
  }

  @Override
  public void handleCommandError(JobClient jobClient, ActivatedJob job, ZeebeBpmnError bpmnError) {

    FinalCommandStep<Void> command = jobClient.newThrowErrorCommand(job)
      .errorCode(bpmnError.getErrorCode())
      .errorMessage(bpmnError.getErrorMessage());

    command.send()
      .exceptionally(t -> {
        this.handleCommandError(jobClient, job, command, t);
        return null;
      });
  }


  private void handleCommandError(JobClient jobClient, ActivatedJob job, FinalCommandStep command, Throwable throwable) {
    // Think about improving error behavior
    // - connection problem -> retry
    // - job non existant any more -> ignore
    throw new RuntimeException("Could not send " + command + " for job " + job + " to Zeebe due to error: " + throwable.getMessage(), throwable);
  }

}
