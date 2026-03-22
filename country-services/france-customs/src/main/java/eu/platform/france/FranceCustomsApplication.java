package eu.platform.france;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class FranceCustomsApplication {

    public static void main(String[] args) {
        SpringApplication.run(FranceCustomsApplication.class, args);
    }

    @Component
    public static class FranceCustomsRoute extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            from("kafka:france-customs-topic?brokers={{kafka.brokers}}")
                .routeId("france-kafka-consumer")
                .log("France Customs received message from Kafka: ${body}")
                .process(exchange -> {
                    // Simulate asynchronous processing
                    System.out.println("Processing Customs declaration in France...");
                });
        }
    }
}
