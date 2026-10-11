package com.apianalyzer.analysis.domain.model.diff;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImpactedConsumer {
    private String name; // e.g. "UserController.getUser" or "OrderService.placeOrder"
    private String type; // "CONTROLLER", "SERVICE", "REPOSITORY", "CLIENT"
    private String filePath;
    private Integer lineNumber;
    private String impactType; // "DIRECT_MATCH", "DOWNSTREAM_DEPENDENCY", "UNVERIFIED"
    private String evidence;
}
