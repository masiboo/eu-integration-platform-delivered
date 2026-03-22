package eu.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossBorderRequest implements Serializable {
    private String requestId;
    private String countryCode; // DE, FR, NL
    private String serviceType; // TAX, CUSTOMS, IDENTITY
    private List<DataEntry> entries;
}
