package io.camunda.zeebe.spring.client.process.generator.bean;

import com.alibaba.fastjson.JSON;
import lombok.Builder;
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
@Builder
public class ProcessDefinition {

  private Process process;

  private BaseDefinition processNode;

  public String toString() {
    return JSON.toJSONString(this);
  }
}
