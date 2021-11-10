package io.camunda.zeebe.spring.client.process.generator.bean.subprocess;

import io.camunda.zeebe.spring.client.process.generator.bean.BaseDefinition;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

/**
 * 〈功能简述〉<br>
 * 〈子流程定义〉
 *
 * @author lizhi
 * @date 2021/11/10
 * @since 1.0.0
 */

@Data
@SuperBuilder
public class SubProcessDefinition extends BaseDefinition {

  @NonNull
  private BaseDefinition childNode;

  @Override
  public String getNodeType() {
    return "subProcess";
  }
}
