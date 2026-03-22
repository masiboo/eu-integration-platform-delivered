package eu.platform.germany;

import eu.platform.common.dto.CrossBorderResponse;
import eu.platform.common.dto.DataEntry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
@RequestMapping("/germany/v1")
public class GermanyTaxApplication {

    public static void main(String[] args) {
        SpringApplication.run(GermanyTaxApplication.class, args);
    }

    @PostMapping("/tax")
    public CrossBorderResponse processTax(@RequestBody DataEntry entry) {
        return CrossBorderResponse.builder()
            .status("SUCCESS")
            .message("Germany Tax Service processed entry: " + entry.getKey())
            .processedBy("Germany-Tax-Internal")
            .build();
    }
}
