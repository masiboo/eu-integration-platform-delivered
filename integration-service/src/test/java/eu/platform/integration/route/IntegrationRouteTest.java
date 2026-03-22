package eu.platform.integration.route;

import eu.platform.common.dto.CrossBorderRequest;
import eu.platform.common.dto.CrossBorderResponse;
import eu.platform.common.dto.DataEntry;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@CamelSpringBootTest
@SpringBootTest
@MockEndpoints("direct:*|http:*|kafka:*")
@UseAdviceWith
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class IntegrationRouteTest {

    @Autowired
    private ProducerTemplate producerTemplate;

    private void adviceAll() throws Exception {
        // Mock all external endpoints and skip real calls
        org.apache.camel.builder.AdviceWith.adviceWith(producerTemplate.getCamelContext(), "germany-tax-route", a -> {
            a.mockEndpointsAndSkip("http:*");
        });
        org.apache.camel.builder.AdviceWith.adviceWith(producerTemplate.getCamelContext(), "france-customs-route", a -> {
            a.mockEndpointsAndSkip("kafka:*");
        });
        org.apache.camel.builder.AdviceWith.adviceWith(producerTemplate.getCamelContext(), "error-handler-route", a -> {
            a.mockEndpointsAndSkip("kafka:*");
        });
        org.apache.camel.builder.AdviceWith.adviceWith(producerTemplate.getCamelContext(), "netherlands-identity-route", a -> {
            a.mockEndpointsAndSkip("http:*");
        });
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    public void testIntegrationSuccessDE() throws Exception {
        adviceAll();
        producerTemplate.getCamelContext().start();

        CrossBorderRequest request = CrossBorderRequest.builder()
                .requestId("REQ-DE-001")
                .countryCode("DE")
                .entries(Collections.singletonList(new DataEntry("taxId", "12345")))
                .build();

        CrossBorderResponse response = producerTemplate.requestBody("direct:germany-route", request, CrossBorderResponse.class);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("Germany-Integration-Adapter", response.getProcessedBy());
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    public void testIntegrationSuccessFR() throws Exception {
        adviceAll();
        producerTemplate.getCamelContext().start();

        CrossBorderRequest request = CrossBorderRequest.builder()
                .requestId("REQ-FR-001")
                .countryCode("FR")
                .entries(Collections.singletonList(new DataEntry("customsId", "FR-999")))
                .build();

        CrossBorderResponse response = producerTemplate.requestBody("direct:france-route", request, CrossBorderResponse.class);

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        assertTrue(response.getMessage().contains("France Customs"));
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    public void testUnsupportedCountry() throws Exception {
        adviceAll();
        producerTemplate.getCamelContext().start();

        CrossBorderRequest request = CrossBorderRequest.builder()
                .requestId("REQ-XX-001")
                .countryCode("XX")
                .entries(Collections.emptyList())
                .build();

        // The error handler should return a failure response
        CrossBorderResponse response = producerTemplate.requestBody("direct:integration-orchestrator", request, CrossBorderResponse.class);
        
        assertNotNull(response);
        assertEquals("FAILURE", response.getStatus());
    }
}
