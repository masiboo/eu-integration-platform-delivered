package eu.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossBorderResponse implements Serializable {
    private String requestId;
    private String status; // SUCCESS, FAILURE
    private String message;
    private String processedBy;
    private Object data;
}
