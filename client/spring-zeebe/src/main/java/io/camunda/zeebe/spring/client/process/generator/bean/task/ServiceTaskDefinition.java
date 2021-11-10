package io.camunda.zeebe.spring.client.process.generator.bean.task;

import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * 〈功能简述〉<br>
 * 〈系统任务定义〉
 *
 * @author lizhi
 * @date 2021-11-10
 * @since 1.0.0
 */

@Data
@SuperBuilder
public class ServiceTaskDefinition extends JobWorkerDefinition {

  @Override
  public String getNodeType() {
    return "serviceTask";
  }
}
