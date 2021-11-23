package io.camunda.zeebe.spring.client.config.processor;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.exception.ZeebeBpmnError;

/**
 * 〈功能简述〉<br>
 * 〈〉
 *
 * @author lizhi
 * @create 2021-11-23
 * @since 1.0.0
 */

public interface CommandHandlingStrategy {

  void handleCommandSuccess(JobClient jobClient, ActivatedJob job, Object result);

  void handleCommandError(JobClient jobClient, ActivatedJob job, ZeebeBpmnError bpmnError);
}
