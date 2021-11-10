package io.camunda.zeebe.spring.client.process.generator.bean.subprocess;

import io.camunda.zeebe.spring.client.process.generator.bean.BaseDefinition;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * 〈功能简述〉<br>
 * 〈〉
 *
 * @author lizhi
 * @date 2021/11/10
 * @since 1.0.0
 */

@Data
@SuperBuilder
public class CallActivityDefinition extends BaseDefinition {

  private String processId;

  private boolean propagateAllChildVariablesEnabled;

  @Override
  public String getNodeType() {
    return "callActivity";
  }
}
