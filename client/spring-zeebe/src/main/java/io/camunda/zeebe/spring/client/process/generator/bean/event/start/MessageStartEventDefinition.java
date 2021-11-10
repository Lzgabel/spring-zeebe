package io.camunda.zeebe.spring.client.process.generator.bean.event.start;

import lombok.Data;
import lombok.NonNull;
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
public class MessageStartEventDefinition extends StartEventDefinition {

  @NonNull
  private String messageName;

  @Override
  public String getEventType() {
    return "message";
  }
}
