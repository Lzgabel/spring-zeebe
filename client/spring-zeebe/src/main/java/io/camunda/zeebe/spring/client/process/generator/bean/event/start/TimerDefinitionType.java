package io.camunda.zeebe.spring.client.process.generator.bean.event.start;

/**
 * 〈功能简述〉<br>
 * 〈〉
 *
 * @author lizhi
 * @create 2021/11/10
 * @since 1.0.0
 */

public enum TimerDefinitionType {

  /**
   * date
   */
  DATE("date"),

  /**
   * cycle
   */
  CYCLE("cycle"),

  /**
   * duration
   */
  DURATION("duration");

  private String value;


  TimerDefinitionType(String value) {
    this.value = value;
  }

  public boolean isEqual(String value) {
    return this.value.equals(value);
  }

  public String value() {
    return this.value;
  }
}
