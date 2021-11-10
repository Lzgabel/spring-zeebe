package io.camunda.zeebe.spring.client.process.generator.bean;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * 〈功能简述〉<br>
 * 〈〉
 *
 * @author lizhi
 * @date 2021-11-10
 * @since 1.0.0
 */

@Data
@SuperBuilder
public abstract class BaseDefinition implements Serializable {

  private String nodeName;

  private static String nodeType;

  private BaseDefinition nextNode;

  public abstract String getNodeType();
}
