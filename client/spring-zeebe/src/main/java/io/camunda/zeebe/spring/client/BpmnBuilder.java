
package io.camunda.zeebe.spring.client;

import cn.lzgabel.BpmnAutoLayout;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.model.bpmn.builder.ProcessBuilder;
import io.camunda.zeebe.model.bpmn.builder.*;
import io.camunda.zeebe.model.bpmn.instance.*;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeCalledElement;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 〈功能简述〉<br>
 * 〈基于 json 格式自动生成 bpmn 文件〉
 *
 * @author lizhi
 * @date 2021-06-15
 * @since 1.0.0
 */

public class BpmnBuilder {

  public static BpmnModelInstance build(String json) {
    return build(json, true);
  }

  public static BpmnModelInstance build(String json, boolean autoLayout) {

    try {
      JSONObject object = JSON.parseObject(json, JSONObject.class);
      ProcessBuilder executableProcess = Bpmn.createExecutableProcess();
      JSONObject workflow = object.getJSONObject("process");
      executableProcess.name(workflow.getString("name")).id(workflow.getString("processId"));

      StartEventBuilder startEventBuilder = executableProcess.startEvent();
      JSONObject flowNode = object.getJSONObject("processNode");
      String lastNode = create(startEventBuilder, startEventBuilder.getElement().getId(), flowNode);

      moveToNode(startEventBuilder, lastNode).endEvent();
      BpmnModelInstance modelInstance = startEventBuilder.done();
      Bpmn.validateModel(modelInstance);
      if (autoLayout) {
        // 自动布局
        return Bpmn.readModelFromStream(
          new ByteArrayInputStream(
            BpmnAutoLayout.layout(Bpmn.convertToString(modelInstance)).getBytes()
          )
        );
      }
      return modelInstance;
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("创建失败: e=" + e.getMessage());
    }
  }


  private static String create(AbstractFlowNodeBuilder startFlowNodeBuilder, String fromId, JSONObject flowNode) throws InvocationTargetException, IllegalAccessException {
    String nodeType = flowNode.getString("nodeType");
    if (Type.PARALLEL.isEqual(nodeType)) {
      return createParallelGatewayBuilder(startFlowNodeBuilder, flowNode);
    } else if (Type.EXCLUSIVE.isEqual(nodeType)) {
      return createExclusiveGatewayBuilder(startFlowNodeBuilder, flowNode);
    } else if (Type.SERVICE_TASK.isEqual(nodeType)) {
      flowNode.put("incoming", Collections.singletonList(fromId));
      String id = createTask(startFlowNodeBuilder, flowNode);

      // 如果当前任务还有后续任务，则遍历创建后续任务
      JSONObject nextNode = flowNode.getJSONObject("nextNode");
      if (Objects.nonNull(nextNode)) {
        AbstractFlowNodeBuilder<?, ?> abstractFlowNodeBuilder = moveToNode(startFlowNodeBuilder, id);
        return create(abstractFlowNodeBuilder, abstractFlowNodeBuilder.getElement().getId(), nextNode);
      } else {
        return id;
      }
    } else if (Type.SUB_PROCESS.isEqual(nodeType)) {
      SubProcessBuilder subProcessBuilder = startFlowNodeBuilder.subProcess();
      EmbeddedSubProcessBuilder embeddedSubProcessBuilder = subProcessBuilder.embeddedSubProcess();
      StartEventBuilder startEventBuilder = embeddedSubProcessBuilder.startEvent();
      subProcessBuilder.getElement().setName(flowNode.getString("nodeName"));
      // 遍历创建子任务
      JSONObject childNode = flowNode.getJSONObject("childNode");
      String lastNode = startEventBuilder.getElement().getId();
      if (Objects.nonNull(childNode)) {
        AbstractFlowNodeBuilder<?, ?> abstractFlowNodeBuilder = moveToNode(subProcessBuilder, startEventBuilder.getElement().getId());
        lastNode = create(abstractFlowNodeBuilder, abstractFlowNodeBuilder.getElement().getId(), childNode);
      }
      moveToNode(startEventBuilder, lastNode).endEvent();

      // 如果当前任务还有后续任务，则遍历创建后续任务
      JSONObject nextNode = flowNode.getJSONObject("nextNode");
      if (Objects.nonNull(nextNode)) {
        AbstractFlowNodeBuilder<?, ?> abstractFlowNodeBuilder = moveToNode(subProcessBuilder, subProcessBuilder.getElement().getId());
        return create(abstractFlowNodeBuilder, abstractFlowNodeBuilder.getElement().getId(), nextNode);
      }
      return subProcessBuilder.getElement().getId();
    } else if (Type.CALL_ACTIVITY.isEqual(nodeType)) {
      CallActivityBuilder callActivityBuilder = startFlowNodeBuilder.callActivity();
      callActivityBuilder.getElement().setName(flowNode.getString("nodeName"));
      callActivityBuilder.addExtensionElement(ZeebeCalledElement.class, (ZeebeCalledElement zeebeCalledElement) -> {
        zeebeCalledElement.setProcessId(flowNode.getString("processId"));
        callActivityBuilder.addExtensionElement(zeebeCalledElement);
      });
      String id = callActivityBuilder.getElement().getId();
      JSONObject childNode = flowNode.getJSONObject("nextNode");
      if (Objects.nonNull(childNode)) {
        AbstractFlowNodeBuilder<?, ?> abstractFlowNodeBuilder = moveToNode(callActivityBuilder, id);
        return create(abstractFlowNodeBuilder, abstractFlowNodeBuilder.getElement().getId(), childNode);
      }
      return id;

    } else {
      throw new RuntimeException("未知节点类型: nodeType=" + nodeType);
    }
  }

  private static String createExclusiveGatewayBuilder(AbstractFlowNodeBuilder startFlowNodeBuilder, JSONObject flowNode) throws InvocationTargetException, IllegalAccessException {
    String name = flowNode.getString("nodeName");
    ExclusiveGatewayBuilder exclusiveGatewayBuilder = startFlowNodeBuilder.exclusiveGateway().name(name);
    if (Objects.isNull(flowNode.getJSONArray("branchNodes")) && Objects.isNull(flowNode.getJSONObject("nextNode"))) {
      return exclusiveGatewayBuilder.getElement().getId();
    }
    List<JSONObject> flowNodes = Optional.ofNullable(flowNode.getJSONArray("branchNodes")).map(e -> e.toJavaList(JSONObject.class)).orElse(Collections.emptyList());
    List<String> incoming = Lists.newArrayListWithCapacity(flowNodes.size());

    List<JSONObject> conditions = Lists.newCopyOnWriteArrayList();
    for (JSONObject element : flowNodes) {
      JSONObject childNode = element.getJSONObject("nextNode");

      String nodeName = element.getString("nodeName");
      String expression = element.getString("conditionExpression");

      // 记录分支条件中不存在任务节点的情况（即空分支）
      if (Objects.isNull(childNode)) {
        incoming.add(exclusiveGatewayBuilder.getElement().getId());
        JSONObject condition = new JSONObject();
        condition.fluentPut("nodeName", nodeName)
          .fluentPut("expression", expression);
        conditions.add(condition);
        continue;
      }
      // 只生成一个任务，同时设置当前任务的条件
      childNode.put("incoming", Collections.singletonList(exclusiveGatewayBuilder.getElement().getId()));
      String identifier = create(exclusiveGatewayBuilder, exclusiveGatewayBuilder.getElement().getId(), childNode);
      exclusiveGatewayBuilder.getElement().getOutgoing().stream().forEach(
        e -> {
          if (StringUtils.isBlank(e.getName()) && StringUtils.isNotBlank(nodeName)) {
            e.setName(nodeName);
          }
          // 设置条件表达式
          if (Objects.isNull(e.getConditionExpression()) && StringUtils.isNotBlank(expression)) {
            Method createInstance = getDeclaredMethod(exclusiveGatewayBuilder, "createInstance", Class.class);
            createInstance.setAccessible(true);
            try {
              ConditionExpression conditionExpression = (ConditionExpression) createInstance.invoke(exclusiveGatewayBuilder, ConditionExpression.class);
              conditionExpression.setTextContent(!expression.isEmpty() && !expression.startsWith("=") ? String.format("=%s", expression) : expression);
              e.setConditionExpression(conditionExpression);
            } catch (IllegalAccessException | InvocationTargetException ex) {
              ex.printStackTrace();
            }
          }
        }
      );
      if (Objects.nonNull(identifier)) {
        incoming.add(identifier);
      }
    }


    JSONObject childNode = flowNode.getJSONObject("nextNode");
    if (Objects.nonNull(childNode)) {
      if (incoming == null || incoming.isEmpty()) {
        return create(exclusiveGatewayBuilder, exclusiveGatewayBuilder.getElement().getId(), childNode);
      } else {
        // 所有 service task 连接 end exclusive gateway
        childNode.put("incoming", incoming);
        AbstractFlowNodeBuilder<?, ?> abstractFlowNodeBuilder = moveToNode(exclusiveGatewayBuilder, incoming.get(0));
        // 1.0 先进行边连接, 暂存 nextNode
        JSONObject nextNode = childNode.getJSONObject("nextNode");
        childNode.put("nextNode", null);
        String identifier = create(abstractFlowNodeBuilder, abstractFlowNodeBuilder.getElement().getId(), childNode);
        for (int i = 1; i < incoming.size(); i++) {
          moveToNode(exclusiveGatewayBuilder, incoming.get(i)).connectTo(identifier);
        }

        //  针对分支条件中空分支场景 添加条件表达式
        if (!conditions.isEmpty()) {
          List<SequenceFlow> sequenceFlows = moveToNode(exclusiveGatewayBuilder, identifier)
            .getElement().getIncoming().stream()
            // 获取从源 gateway 到目标节点 未设置条件表达式的节点
            .filter(e -> StringUtils.equals(e.getSource().getId(), exclusiveGatewayBuilder.getElement().getId()))
            .collect(Collectors.toList());

          sequenceFlows.stream().forEach(sequenceFlow -> {
            if (!conditions.isEmpty()) {
              JSONObject condition = conditions.get(0);
              String nodeName = condition.getString("nodeName");
              String expression = condition.getString("expression");

              if (StringUtils.isBlank(sequenceFlow.getName()) && StringUtils.isNotBlank(nodeName)) {
                sequenceFlow.setName(nodeName);
              }
              // 设置条件表达式
              if (Objects.isNull(sequenceFlow.getConditionExpression()) && StringUtils.isNotBlank(expression)) {
                Method createInstance = getDeclaredMethod(exclusiveGatewayBuilder, "createInstance", Class.class);
                createInstance.setAccessible(true);
                try {
                  ConditionExpression conditionExpression = (ConditionExpression) createInstance.invoke(exclusiveGatewayBuilder, ConditionExpression.class);
                  conditionExpression.setTextContent(!expression.isEmpty() && !expression.startsWith("=") ? String.format("=%s", expression) : expression);
                  sequenceFlow.setConditionExpression(conditionExpression);
                } catch (IllegalAccessException | InvocationTargetException ex) {
                  ex.printStackTrace();
                }
              }

              conditions.remove(0);
            }
          });

        }

        // 1.1 边连接完成后，在进行 nextNode 创建
        if (Objects.nonNull(nextNode)) {
          return create(moveToNode(exclusiveGatewayBuilder, identifier), identifier, nextNode);
        } else {
          return identifier;
        }
      }
    }
    return exclusiveGatewayBuilder.getElement().getId();
  }

  private static String createParallelGatewayBuilder(AbstractFlowNodeBuilder startFlowNodeBuilder, JSONObject flowNode) throws InvocationTargetException, IllegalAccessException {
    String name = flowNode.getString("nodeName");
    ParallelGatewayBuilder parallelGatewayBuilder = startFlowNodeBuilder.parallelGateway().name(name);
    if (Objects.isNull(flowNode.getJSONArray("branchNodes"))
      && Objects.isNull(flowNode.getJSONObject("nextNode"))) {
      return parallelGatewayBuilder.getElement().getId();
    }

    List<JSONObject> flowNodes = Optional.ofNullable(flowNode.getJSONArray("branchNodes")).map(e -> e.toJavaList(JSONObject.class)).orElse(Collections.emptyList());
    List<String> incoming = Lists.newArrayListWithCapacity(flowNodes.size());
    for (JSONObject element : flowNodes) {
      JSONObject childNode = element.getJSONObject("nextNode");
      if (Objects.isNull(childNode)) {
        incoming.add(parallelGatewayBuilder.getElement().getId());
        continue;
      }
      String identifier = create(parallelGatewayBuilder, parallelGatewayBuilder.getElement().getId(), childNode);
      if (Objects.nonNull(identifier)) {
        incoming.add(identifier);
      }
    }

    JSONObject childNode = flowNode.getJSONObject("nextNode");
    if (Objects.nonNull(childNode)) {
      // 普通结束网关
      if (incoming == null || incoming.size() == 0) {
        return create(parallelGatewayBuilder, parallelGatewayBuilder.getElement().getId(), childNode);
      } else {
        // 所有 service task 连接 end parallel gateway
        childNode.put("incoming", incoming);
        AbstractFlowNodeBuilder<?, ?> abstractFlowNodeBuilder = moveToNode(parallelGatewayBuilder, incoming.get(0));
        // 1.0 先进行边连接, 暂存 nextNode
        JSONObject nextNode = childNode.getJSONObject("nextNode");
        childNode.put("nextNode", null);
        String identifier = create(abstractFlowNodeBuilder, abstractFlowNodeBuilder.getElement().getId(), childNode);
        for (int i = 1; i < incoming.size(); i++) {
          moveToNode(parallelGatewayBuilder, incoming.get(i)).connectTo(identifier);
        }
        // 1.1 边连接完成后，在进行 nextNode 创建
        if (Objects.nonNull(nextNode)) {
          return create(moveToNode(parallelGatewayBuilder, identifier), identifier, nextNode);
        } else {
          return identifier;
        }
      }
    }
    return parallelGatewayBuilder.getElement().getId();
  }

  private static String createTask(AbstractFlowNodeBuilder startFlowNodeBuilder, JSONObject flowNode) throws InvocationTargetException, IllegalAccessException {
    Map<String, AbstractFlowNodeBuilder> map = Maps.newHashMap();
    String nodeType = flowNode.getString("nodeType");
    String jobType = flowNode.getString("jobType");
    List<String> incoming = flowNode.getJSONArray("incoming").toJavaList(String.class);
    ServiceTaskBuilder serviceTaskBuilder = null;
    if (incoming != null && !incoming.isEmpty()) {

      // 创建 serviceTask
      AbstractFlowNodeBuilder<?, ?> abstractFlowNodeBuilder = moveToNode(startFlowNodeBuilder, incoming.get(0));
      // 自动生成id
      Method createTarget = getDeclaredMethod(abstractFlowNodeBuilder, "createTarget", Class.class);
      // 手动传入id
      //Method createTarget = getDeclaredMethod(abstractFlowNodeBuilder, "createTarget", Class.class, String.class);
      createTarget.setAccessible(true);
      Object target = createTarget.invoke(abstractFlowNodeBuilder, Type.TYPE_MAP.get(nodeType));
      if (target instanceof ServiceTask) {
        ServiceTask serviceTask = (ServiceTask) target;
        serviceTask.setName(flowNode.getString("nodeName"));
        serviceTaskBuilder = serviceTask.builder().zeebeJobType(jobType);

        // 补充header
        if (flowNode.containsKey("taskHeaders")) {
          Map<String, String> taskHeaders = Optional.ofNullable(flowNode.getJSONObject("taskHeaders")).map(e -> e.toJavaObject(Map.class)).orElse(Maps.newHashMap());
          final ServiceTaskBuilder finalServiceTaskBuilder = serviceTaskBuilder;
          taskHeaders.entrySet().stream().filter(entry -> StringUtils.isNotBlank(entry.getKey()))
            .forEach(entry -> finalServiceTaskBuilder.zeebeTaskHeader(entry.getKey(), entry.getValue()));
        }

        for (int i = 1; i < incoming.size(); i++) {
          abstractFlowNodeBuilder = moveToNode(startFlowNodeBuilder, incoming.get(i));
          abstractFlowNodeBuilder.connectTo(serviceTaskBuilder.getElement().getId());
        }
      }
    }
    return serviceTaskBuilder.getElement().getId();
  }

  /**
   * 循环向上转型, 获取对象的 DeclaredMethod
   *
   * @param object         : 子类对象
   * @param methodName     : 父类中的方法名
   * @param parameterTypes : 父类中的方法参数类型
   * @return 父类中的方法对象
   */
  private static Method getDeclaredMethod(Object object, String methodName, Class<?>... parameterTypes) {
    Method method = null;
    for (Class<?> clazz = object.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
      try {
        method = clazz.getDeclaredMethod(methodName, parameterTypes);
        return method;
      } catch (Exception e) {
        //这里甚么都不要做！并且这里的异常必须这样写，不能抛出去。
        //如果这里的异常打印或者往外抛，则就不会执行clazz = clazz.getSuperclass(),最后就不会进入到父类中了
      }
    }
    return null;
  }


  private static AbstractFlowNodeBuilder<?, ?> moveToNode(AbstractFlowNodeBuilder<?, ?> startEventBuilder, String identifier) {
    return startEventBuilder.moveToNode(identifier);
  }

  private enum Type {

    /**
     * 并行事件
     */
    PARALLEL("parallel", ParallelGateway.class),

    /**
     * 排他事件
     */
    EXCLUSIVE("exclusive", ExclusiveGateway.class),

    /**
     * 任务
     */
    SERVICE_TASK("serviceTask", ServiceTask.class),

    /**
     * 子任务类型
     */
    SUB_PROCESS("subProcess", SubProcess.class),

    /**
     * 调用任务类型
     */
    CALL_ACTIVITY("callActivity", CallActivity.class);

    private String type;

    private Class<?> typeClass;

    Type(String type, Class<?> typeClass) {
      this.type = type;
      this.typeClass = typeClass;
    }

    public final static Map<String, Class<?>> TYPE_MAP = Maps.newHashMap();

    static {
      for (Type element : Type.values()) {
        TYPE_MAP.put(element.type, element.typeClass);
      }
    }

    public boolean isEqual(String type) {
      return this.type.equals(type);
    }

  }
}
