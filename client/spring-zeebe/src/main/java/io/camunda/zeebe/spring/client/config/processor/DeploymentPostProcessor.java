package io.camunda.zeebe.spring.client.config.processor;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.command.DeployProcessCommandStep1;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.spring.client.annotation.ZeebeDeployment;
import io.camunda.zeebe.spring.client.bean.ClassInfo;
import io.camunda.zeebe.spring.client.bean.value.ZeebeDeploymentValue;
import io.camunda.zeebe.spring.client.bean.value.factory.ReadZeebeDeploymentValue;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

public class DeploymentPostProcessor extends BeanInfoPostProcessor {

  private static final Logger LOGGER =
    LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final ReadZeebeDeploymentValue reader;

  private static final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

  public DeploymentPostProcessor(ReadZeebeDeploymentValue reader) {
    this.reader = reader;
  }

  @Override
  public boolean test(final ClassInfo beanInfo) {
    return beanInfo.hasClassAnnotation(ZeebeDeployment.class);
  }

  @Override
  public Consumer<ZeebeClient> apply(final ClassInfo beanInfo) {
    final ZeebeDeploymentValue value = reader.applyOrThrow(beanInfo);

    LOGGER.info("deployment: {}", value);

    return client -> {

      DeployProcessCommandStep1 deployWorkflowCommand = client
        .newDeployCommand();

      List<Resource> locations = Stream.of(Optional.ofNullable(value.getBpmnLocations()).orElse(Collections.emptyList()))
        .flatMap(List::stream)
        .flatMap(location -> Stream.of(getResources(location)))
        .collect(Collectors.toList());

      Stream<DeployProcessCommandStep1.DeployProcessCommandBuilderStep2> deployProcessCommandBuilderStep2StreamOfLocations = locations.stream().map(resource -> {
        try {
          return deployWorkflowCommand.addResourceStream(resource.getInputStream(), resource.getFilename());
        } catch (IOException e) {
        }
        return null;
      }).filter(Objects::nonNull);

      Stream<DeployProcessCommandStep1.DeployProcessCommandBuilderStep2> deployProcessCommandBuilderStep2StreamOfResources = value.getClassPathResources()
        .stream()
        .map(deployWorkflowCommand::addResourceFromClasspath);

      DeploymentEvent deploymentResult =  Stream.of(deployProcessCommandBuilderStep2StreamOfLocations, deployProcessCommandBuilderStep2StreamOfResources)
        .flatMap(u -> u)
        .reduce((first, second) -> second)
        .orElseThrow(() -> new IllegalArgumentException("Requires at least one resource to deploy"))
        .send()
        .join();

      LOGGER.info(
        "Deployed: {}",
        deploymentResult
          .getProcesses()
          .stream()
          .map(wf -> String.format("<%s:%d>", wf.getBpmnProcessId(), wf.getVersion()))
          .collect(Collectors.joining(",")));
    };
  }

  private Resource[] getResources(String location) {
    try {
      return resourceResolver.getResources(location);
    } catch (IOException e) {
      return new Resource[0];
    }
  }
}
