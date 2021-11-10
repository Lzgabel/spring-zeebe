package io.camunda.zeebe.spring.client.process.generator.bean.task;

import com.alibaba.fastjson.JSONObject;
import io.camunda.zeebe.spring.client.process.generator.bean.BaseDefinition;
import lombok.Data;
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
public class UserTaskDefinition extends BaseDefinition {

  private String assignee;

  private String candidateGroups;

  private String userTaskForm;

  public JSONObject taskHeaders;

  @Override
  public String getNodeType() {
    return "userTask";
  }
}
