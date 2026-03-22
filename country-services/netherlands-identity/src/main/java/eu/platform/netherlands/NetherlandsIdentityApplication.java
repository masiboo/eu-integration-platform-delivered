package eu.platform.netherlands;

import eu.platform.common.dto.CrossBorderRequest;
import eu.platform.common.dto.CrossBorderResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
@RequestMapping("/netherlands/v1")
public class NetherlandsIdentityApplication {

    public static void main(String[] args) {
        SpringApplication.run(NetherlandsIdentityApplication.class, args);
    }

    @PostMapping("/identity")
    public CrossBorderResponse verifyIdentity(@RequestBody CrossBorderRequest request) {
        return CrossBorderResponse.builder()
            .requestId(request.getRequestId())
            .status("SUCCESS")
            .message("Netherlands Identity Service verified request")
            .processedBy("NL-DigiD-Simulator")
            .build();
    }
}
