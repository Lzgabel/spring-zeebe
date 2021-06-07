package io.camunda.zeebe.spring.client.bean.value;

import io.camunda.zeebe.spring.client.bean.ClassInfo;
import java.util.List;
import java.util.Objects;

public class ZeebeDeploymentValue implements ZeebeAnnotationValue<ClassInfo> {

  private List<String> classPathResources;

  private List<String> bpmnLocations;

  private ClassInfo beanInfo;

  private ZeebeDeploymentValue(List<String> classPathResources, ClassInfo beanInfo, List<String> bpmnLocations) {
    this.classPathResources = classPathResources;
    this.beanInfo = beanInfo;
    this.bpmnLocations = bpmnLocations;
  }

  public List<String> getClassPathResources() {
    return classPathResources;
  }

  public List<String> getBpmnLocations() {
    return bpmnLocations;
  }

  @Override
  public ClassInfo getBeanInfo() {
    return beanInfo;
  }

  public List<String> getBpmnLocations() {
    return bpmnLocations;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ZeebeDeploymentValue that = (ZeebeDeploymentValue) o;
    return Objects.equals(classPathResources, that.classPathResources) &&
      Objects.equals(beanInfo, that.beanInfo) &&
      Objects.equals(bpmnLocations, that.bpmnLocations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(classPathResources, beanInfo, bpmnLocations);
  }

  @Override
  public String toString() {
    return "ZeebeDeploymentValue{" +
      "classPathResources=" + classPathResources +
      ", beanInfo=" + beanInfo +
      ", bpmnLocations=" + bpmnLocations +
      '}';
  }

  public static ZeebeDeploymentValueBuilder builder() {
    return new ZeebeDeploymentValueBuilder();
  }

  public static final class ZeebeDeploymentValueBuilder {

    private List<String> classPathResources;
    private List<String> bpmnLocations;
    private ClassInfo beanInfo;

    private ZeebeDeploymentValueBuilder() {
    }

    public ZeebeDeploymentValueBuilder classPathResources(List<String> classPathResources) {
      this.classPathResources = classPathResources;
      return this;
    }

    public ZeebeDeploymentValueBuilder beanInfo(ClassInfo beanInfo) {
      this.beanInfo = beanInfo;
      return this;
    }

    public ZeebeDeploymentValueBuilder bpmnLocations(List<String> bpmnLocations) {
      this.bpmnLocations = bpmnLocations;
      return this;
    }

    public ZeebeDeploymentValue build() {
      return new ZeebeDeploymentValue(classPathResources, beanInfo, bpmnLocations);
    }
  }
}
