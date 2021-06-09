package io.camunda.zeebe.spring.client.config;

import com.google.common.collect.Lists;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.ZeebeClientBuilder;
import io.camunda.zeebe.client.impl.ZeebeClientBuilderImpl;
import io.camunda.zeebe.spring.client.ZeebeClientLifecycle;
import io.camunda.zeebe.spring.client.ZeebeClientObjectFactory;
import io.camunda.zeebe.spring.client.bean.value.factory.ReadAnnotationValueConfiguration;
import io.camunda.zeebe.spring.client.config.processor.PostProcessorConfiguration;
import io.camunda.zeebe.spring.client.properties.GatewayProperties;
import io.camunda.zeebe.spring.client.properties.ZeebeClientProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Import({
  PostProcessorConfiguration.class,
  ReadAnnotationValueConfiguration.class,

})
public class ZeebeClientSpringConfiguration {

  @Autowired
  List<ZeebeClientBuilder> zeebeClientBuilders;

  @Autowired
  ZeebeClientProperties zeebeClientProperties;

  @Autowired
  DefaultListableBeanFactory beanFactory;

  public static final ZeebeClientBuilderImpl DEFAULT =
    (ZeebeClientBuilderImpl) new ZeebeClientBuilderImpl().withProperties(new Properties());

  @Bean
  public List<ZeebeClientLifecycle> zeebeClientLifecycles(
    final List<ZeebeClientObjectFactory> factories,
    final ApplicationEventPublisher publisher) {
    List<GatewayProperties> gateways = zeebeClientProperties.getGateways();
    if (CollectionUtils.isEmpty(gateways)) {
      if (StringUtils.isEmpty(zeebeClientProperties.getGatewayAddress())) {
        throw new RuntimeException("The 'single gatewayAddress' or 'multiple gateways' can't be empty");
      }
    }
    Map<String, String> addressMap = gateways.stream().collect(Collectors.toMap(GatewayProperties::getAddress, GatewayProperties::getName, (v1, v2)->v2));
    if (!addressMap.containsValue("master")) {
      throw new RuntimeException("The 'multiple gateways' must contain 'master' name");
    }
    return factories.stream().map(factory -> {
      ZeebeClient zeebeClient = factory.getObject();
      final String gatewayAddress = zeebeClient.getConfiguration().getGatewayAddress();
      final String name = addressMap.get(gatewayAddress);
      ZeebeClientLifecycle zeebeClientLifecycle = new ZeebeClientLifecycle(factory, publisher);
      beanFactory.registerSingleton(name + "ZeebeClientLifecycle", zeebeClientLifecycle);
      beanFactory.registerSingleton(name + "ZeebeClient", zeebeClient);
      return zeebeClientLifecycle;
    }).collect(Collectors.toList());
  }

  @Bean
  public List<ZeebeClientObjectFactory> zeebeClientObjectFactory() {
    List<ZeebeClientObjectFactory> factories = Lists.newArrayList();
    zeebeClientBuilders.forEach(zeebeClientBuilder -> factories.add(() -> zeebeClientBuilder.build()));
    return factories;
  }
}
