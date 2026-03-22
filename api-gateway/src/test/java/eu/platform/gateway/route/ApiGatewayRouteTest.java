package eu.platform.gateway.route;

import eu.platform.common.dto.CrossBorderRequest;
import eu.platform.common.dto.CrossBorderResponse;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@CamelSpringBootTest
@SpringBootTest
@MockEndpoints("direct:*|http:*|kafka:*")
@UseAdviceWith
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ApiGatewayRouteTest {

    @Autowired
    private ProducerTemplate producerTemplate;

    @Test
    public void testGatewayAuthFailure() throws Exception {
        // Start the context manually due to UseAdviceWith
        producerTemplate.getCamelContext().start();

        CrossBorderRequest request = CrossBorderRequest.builder()
                .requestId("123")
                .countryCode("DE")
                .entries(Collections.emptyList())
                .build();

        // No Auth Header
        CrossBorderResponse response = producerTemplate.requestBody("direct:process-request", request, CrossBorderResponse.class);
        
        assertEquals("FAILURE", response.getStatus());
        assertTrue(response.getMessage().contains("Unauthorized"));
    }

    @Test
    public void testGatewaySuccessForwarding() throws Exception {
        // Mock the HTTP endpoint response using AdviceWith
        org.apache.camel.builder.AdviceWith.adviceWith(producerTemplate.getCamelContext(), "gateway-forwarder", a -> {
            a.mockEndpointsAndSkip("http:*");
        });

        // Start the context manually due to UseAdviceWith
        producerTemplate.getCamelContext().start();

        org.apache.camel.component.mock.MockEndpoint mockHttp = producerTemplate.getCamelContext().getEndpoint("mock:http:localhost:8081/integration/v1/process", org.apache.camel.component.mock.MockEndpoint.class);
        mockHttp.whenAnyExchangeReceived(exchange -> {
            exchange.getMessage().setBody(CrossBorderResponse.builder()
                .status("SUCCESS")
                .message("Forwarded successfully")
                .build());
        });

        CrossBorderRequest request = CrossBorderRequest.builder()
                .requestId("123")
                .countryCode("DE")
                .entries(Collections.emptyList())
                .build();

        // With Mock Auth Header
        CrossBorderResponse response = producerTemplate.requestBodyAndHeader(
                "direct:process-request", 
                request, 
                "Authorization", 
                "Bearer mock-token", 
                CrossBorderResponse.class);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
    }
}
