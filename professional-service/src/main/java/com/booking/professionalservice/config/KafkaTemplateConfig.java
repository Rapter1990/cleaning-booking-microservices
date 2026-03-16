package com.booking.professionalservice.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Explicit KafkaTemplate bean.
 *
 * <p>In this codebase we reference KafkaTemplate in DLQ config. Depending on how
 * the app is started (and what auto-config is excluded), Spring Boot may not
 * create a KafkaTemplate automatically. This configuration guarantees a template
 * exists when Kafka integration is enabled.</p>
 *
 * <p>We publish JSON payloads as Strings, so we force String serializers.</p>
 */
@Configuration
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(prefix = "justlife.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTemplateConfig {

  @Bean
  @ConditionalOnMissingBean(KafkaTemplate.class)
  public KafkaTemplate<Object, Object> kafkaTemplate(Environment env) {
    String bootstrap = env.getProperty("spring.kafka.bootstrap-servers", "localhost:9092");

    Map<String, Object> config = new HashMap<>();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

    // Sensible dev defaults; can be overridden via spring.kafka.producer.properties.*
    config.putIfAbsent(ProducerConfig.ACKS_CONFIG, env.getProperty("spring.kafka.producer.acks", "all"));

    DefaultKafkaProducerFactory<Object, Object> pf = new DefaultKafkaProducerFactory<>(config);
    return new KafkaTemplate<>(pf);
  }

}
