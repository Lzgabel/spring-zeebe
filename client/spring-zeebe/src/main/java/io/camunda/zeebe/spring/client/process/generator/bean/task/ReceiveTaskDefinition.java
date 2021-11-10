package io.camunda.zeebe.spring.client.process.generator.bean.task;

import io.camunda.zeebe.spring.client.process.generator.bean.BaseDefinition;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

/**
 * 〈功能简述〉<br>
 * 〈接收任务定义〉
 *
 * @author lizhi
 * @date 2021/11/10
 * @since 1.0.0
 */

@Data
@SuperBuilder
public class ReceiveTaskDefinition extends BaseDefinition {

  @NonNull
  private String messageName;

  @NonNull
  private String correlationKey;

  @Override
  public String getNodeType() {
    return "receiveTask";
  }
}
