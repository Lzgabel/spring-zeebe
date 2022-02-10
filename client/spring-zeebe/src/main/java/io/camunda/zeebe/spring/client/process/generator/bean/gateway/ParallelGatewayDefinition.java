package io.camunda.zeebe.spring.client.process.generator.bean.gateway;

import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * 〈功能简述〉<br>
 * 〈并行网关定义〉
 *
 * @author lizhi
 * @date 2021/11/10
 * @since 1.0.0
 */

@Data
@SuperBuilder
public class ParallelGatewayDefinition extends GatewayDefinition {

  @Override
  public String getNodeType() {
    return "parallelGateway";
  }
}