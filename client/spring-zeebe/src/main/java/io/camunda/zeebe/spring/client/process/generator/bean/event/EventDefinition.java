package io.camunda.zeebe.spring.client.process.generator.bean.event;

import io.camunda.zeebe.spring.client.process.generator.bean.BaseDefinition;
import lombok.experimental.SuperBuilder;

/**
 * 〈功能简述〉<br>
 * 〈〉
 *
 * @author lizhi
 * @date 2021/11/10
 * @since 1.0.0
 */

@SuperBuilder
public abstract class EventDefinition extends BaseDefinition {

  private String eventType;

  public abstract String getEventType();
}
