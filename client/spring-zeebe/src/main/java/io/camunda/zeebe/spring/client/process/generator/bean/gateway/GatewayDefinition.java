package io.camunda.zeebe.spring.client.process.generator.bean.gateway;

import io.camunda.zeebe.spring.client.process.generator.bean.BaseDefinition;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;

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
public abstract class GatewayDefinition extends BaseDefinition {

  private List<BranchNode> branchNodes;

}
