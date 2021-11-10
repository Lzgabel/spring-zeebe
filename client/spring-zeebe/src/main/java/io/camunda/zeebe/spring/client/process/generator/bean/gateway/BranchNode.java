package io.camunda.zeebe.spring.client.process.generator.bean.gateway;

import io.camunda.zeebe.spring.client.process.generator.bean.BaseDefinition;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * 〈功能简述〉<br>
 * 〈分支节点〉
 *
 * @author lizhi
 * @date 2021/11/10
 * @since 1.0.0
 */

@Data
@SuperBuilder
public class BranchNode {

  private String nodeName;

  private String conditionExpression;

  private BaseDefinition nextNode;

}
