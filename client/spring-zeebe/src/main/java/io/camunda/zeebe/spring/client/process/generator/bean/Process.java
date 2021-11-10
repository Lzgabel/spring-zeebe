package io.camunda.zeebe.spring.client.process.generator.bean;

import lombok.Data;

/**
 * 〈功能简述〉<br>
 * 〈〉
 *
 * @author lizhi
 * @date 2021-11-10
 * @since 1.0.0
 */

@Data
public class Process {

  /**
   * 流程id
   */
  private String processId;

  /**
   * 流程名称
   */
  private String name;

  /**
   * The attribute specifies which group(s) will be able to start the process.
   */
  private String candidateStarterGroups;

  /**
   * The attribute specifies which user(s) will be able to start the process.
   */
  private String candidateStarterUsers;

}
