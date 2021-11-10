package io.camunda.zeebe.spring.client.process.generator.bean.task;

import com.alibaba.fastjson.JSONObject;
import io.camunda.zeebe.spring.client.process.generator.bean.BaseDefinition;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * 〈功能简述〉<br>
 * 〈JobWorker定义〉
 *
 * @author lizhi
 * @date 2021-11-10
 * @since 1.0.0
 */

@Data
@SuperBuilder
public abstract class JobWorkerDefinition extends BaseDefinition {

  private String jobType;

  private String jobRetries = "3";

  public JSONObject taskHeaders;

}
