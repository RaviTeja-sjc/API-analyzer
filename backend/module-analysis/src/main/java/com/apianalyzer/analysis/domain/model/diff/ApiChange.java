package com.apianalyzer.analysis.domain.model.diff;

import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiChange {
    private ChangeType type;
    private ChangeSeverity severity;
    private String description;
    private String path;
    private String method;
    private String oldValue;
    private String newValue;
    private ImpactClassification impact;

    @Builder.Default
    private List<ImpactedConsumer> impactedConsumers = new ArrayList<>();
}
