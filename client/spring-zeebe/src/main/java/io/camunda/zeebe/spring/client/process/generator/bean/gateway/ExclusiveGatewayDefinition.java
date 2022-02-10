package io.camunda.zeebe.spring.client.process.generator.bean.gateway;

import lombok.experimental.SuperBuilder;

/**
 * 〈功能简述〉<br>
 * 〈排他网关定义〉
 *
 * @author lizhi
 * @date 2021/11/10
 * @since 1.0.0
 */

@SuperBuilder
public class ExclusiveGatewayDefinition extends GatewayDefinition {

  @Override
  public String getNodeType() {
    return "exclusiveGateway";
  }
}