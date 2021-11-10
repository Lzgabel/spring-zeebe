package io.camunda.zeebe.spring.client.process.generator.bean;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

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
public class Process {

  /**
   * 流程id
   */
  private String processId;

  /**
   * 流程名称
   */
  private String name;
}
