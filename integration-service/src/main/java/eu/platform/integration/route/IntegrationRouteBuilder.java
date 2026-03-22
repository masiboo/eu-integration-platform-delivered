package eu.platform.integration.route;

import eu.platform.common.dto.CrossBorderRequest;
import eu.platform.common.dto.CrossBorderResponse;
import eu.platform.common.dto.DataEntry;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class IntegrationRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Configure REST DSL for Integration Service
        restConfiguration()
            .component("servlet")
            .bindingMode(RestBindingMode.json)
            .contextPath("/integration");

        // 1. Dead Letter Channel (Failure Handling)
        errorHandler(deadLetterChannel("direct:error-handler")
            .maximumRedeliveries(3)
            .redeliveryDelay(1000)
            .retryAttemptedLogLevel(org.apache.camel.LoggingLevel.WARN));

        from("direct:error-handler")
            .routeId("error-handler-route")
            .log("ERROR: Moving failed exchange ${exchangeId} to dead letter queue")
            .setBody(constant(CrossBorderResponse.builder()
                .status("FAILURE")
                .message("Internal Processing Error")
                .build()))
            .to("kafka:dead-letter-topic?brokers={{kafka.brokers}}&lazyStartProducer=true");

        // Main Integration REST Endpoint
        rest("/v1")
            .post("/process")
                .type(CrossBorderRequest.class)
                .to("direct:integration-orchestrator");

        // 2. Integration Orchestrator (CBR + Splitter + Aggregator)
        from("direct:integration-orchestrator")
            .routeId("integration-orchestrator")
            .setHeader("requestId", simple("${body.requestId}"))
            .log("Orchestrating request ${header.requestId} for country ${body.countryCode}")

            // 3. Content-Based Router (CBR)
            .choice()
                .when(simple("${body.countryCode} == 'DE'"))
                    .to("direct:germany-route")
                .when(simple("${body.countryCode} == 'FR'"))
                    .to("direct:france-route")
                .when(simple("${body.countryCode} == 'NL'"))
                    .to("direct:netherlands-route")
                .otherwise()
                    .throwException(new IllegalArgumentException("Unsupported EU Country Code"))
            .end();

        // 4. Germany Route: Message Translator (JSON to XML mock) + Splitter
        from("direct:germany-route")
            .routeId("germany-tax-route")
            .log("Processing German Tax Data")
            // 5. Splitter EIP: Split data entries to process individually
            .split(body().method("getEntries"))
                .parallelProcessing()
                .log("Processing single entry: ${body.key}")
                // Mock call to Germany Tax Service (Internal REST call)
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .to("http://localhost:8082/germany/v1/tax?bridgeEndpoint=true")
            .end()
            // 6. Aggregator EIP (Simplified by split-end)
            .setBody(constant(CrossBorderResponse.builder()
                .requestId(UUID.randomUUID().toString())
                .status("SUCCESS")
                .processedBy("Germany-Integration-Adapter")
                .message("German tax data processed successfully")
                .build()));

        // France Route: Direct Kafka Integration
        from("direct:france-route")
            .routeId("france-customs-route")
            .log("Forwarding to France Customs via Kafka")
            .marshal().json()
            .to("kafka:france-customs-topic?brokers={{kafka.brokers}}&lazyStartProducer=true")
            .setBody(constant(CrossBorderResponse.builder()
                .status("PENDING")
                .message("Request forwarded to France Customs asynchronously")
                .build()));

        // Netherlands Route: Identity Verification
        from("direct:netherlands-route")
            .routeId("netherlands-identity-route")
            .log("Calling Netherlands Identity Service")
            .to("http://localhost:8084/netherlands/v1/identity?bridgeEndpoint=true")
            .unmarshal().json(CrossBorderResponse.class);
    }
}
