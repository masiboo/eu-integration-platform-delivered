package eu.platform.gateway.route;

import eu.platform.common.dto.CrossBorderRequest;
import eu.platform.common.dto.CrossBorderResponse;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

@Component
public class ApiGatewayRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        
        // Configure REST DSL
        restConfiguration()
            .component("servlet")
            .bindingMode(RestBindingMode.json);

        // Global Exception Handling
        onException(Exception.class)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
            .setBody(constant(CrossBorderResponse.builder()
                .status("FAILURE")
                .message("Internal Server Error in Gateway")
                .build()));

        onException(SecurityException.class)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(401))
            .setBody(constant(CrossBorderResponse.builder()
                .status("FAILURE")
                .message("Unauthorized: Missing or invalid token")
                .build()));

        onException(org.apache.camel.processor.ThrottlerRejectedExecutionException.class)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(429))
            .setBody(constant(CrossBorderResponse.builder()
                .status("FAILURE")
                .message("Too Many Requests: Rate limit exceeded")
                .build()));

        // REST Endpoints
        rest("/v1/exchange")
            .post("/")
                .description("Submit a cross-border data exchange request")
                .type(CrossBorderRequest.class)
                .outType(CrossBorderResponse.class)
                .to("direct:process-request");

        // Route: process-request
        from("direct:process-request")
            .routeId("gateway-main-route")
            .log("Gateway received request: ${body.requestId} from ${body.countryCode}")
            
            // 1. Mock Authentication (JWT Validation)
            .process(exchange -> {
                String authHeader = exchange.getIn().getHeader("Authorization", String.class);
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    throw new SecurityException("Missing or invalid Authorization header");
                }
                // Simulate JWT validation
                exchange.getIn().setHeader("validatedUser", "EU_MEMBER_STATE_SYSTEM");
            })

            // 2. Rate Limiting (Throttler EIP)
            // Limit to 10 requests per 10 seconds per country (simplified)
            .throttle(10).timePeriodMillis(10000)
                .rejectExecution(true)
            
            // 3. Forward to Integration Service (Simulate via Direct/HTTP)
            // In a real microservice setup, this would be an HTTP call or Kafka message
            .to("direct:forward-to-integration");

        from("direct:forward-to-integration")
            .routeId("gateway-forwarder")
            .log("Forwarding request ${body.requestId} to Integration Service")
            // For demo purposes, we call a mock endpoint or direct route
            // In full implementation, this would call the integration-service
            .to("http://localhost:8081/integration/v1/process?bridgeEndpoint=true");
    }
}
