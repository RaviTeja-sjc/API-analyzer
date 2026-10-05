package com.apianalyzer.analysis.application.service.report;
import com.apianalyzer.analysis.application.dto.AnalysisReportDto.*;
import com.apianalyzer.analysis.application.dto.AnalysisReportDto;
import com.apianalyzer.analysis.domain.model.diff.ApiChange;
import com.apianalyzer.analysis.domain.model.diff.ApiConsumerImpact;
import com.apianalyzer.analysis.domain.model.diff.ChangeSeverity;
import com.apianalyzer.analysis.domain.model.graph.ImpactGraph;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
@Service
public class AnalysisReportBuilderService {
    
    public AnalysisReportDto buildReport(String baseVer, String headVer, List<ApiChange> changes, List<ApiConsumerImpact> impacts, ImpactGraph graph) {
        AnalysisReportDto report = new AnalysisReportDto();
        report.setBaseVersion(baseVer);
        report.setHeadVersion(headVer);
        
        ReportSummary summary = new ReportSummary();
        summary.setTotalChanges(changes.size());
        summary.setBreakingChanges((int) changes.stream().filter(c -> c.getSeverity() == ChangeSeverity.BREAKING).count());
        summary.setPotentiallyBreakingChanges((int) changes.stream().filter(c -> c.getSeverity() == ChangeSeverity.POTENTIALLY_BREAKING).count());
        summary.setTotalImpactedConsumers(impacts.size()); // Simplifying distinct count
        report.setSummary(summary);
        
        for (ApiChange change : changes) {
            ChangeReport cr = ChangeReport.builder()
                .endpointMethod(change.getMethod())
                .endpointPath(change.getPath())
                .changeType(change.getType().name())
                .breakingStatus(change.getSeverity().name())
                .description(change.getDescription())
                .oldValue(change.getOldValue())
                .newValue(change.getNewValue())
                .build();
                
            if (change.getImpact() != null) {
                cr.setSeverityLevel(change.getImpact().getLevel().name());
                cr.setImpactScore(change.getImpact().getScore());
                cr.setImpactReasons(change.getImpact().getReasons());
            }
            
            // Warnings & Recommendations
            if (change.getSeverity() == ChangeSeverity.BREAKING) {
                cr.getWarnings().add("This change will break downstream clients.");
                cr.getRecommendations().add("Consider versioning the API endpoint instead of modifying it directly.");
            }
            
            // Attach impacts
            List<ApiConsumerImpact> relatedImpacts = impacts.stream().filter(i -> i.getRelatedChange() == change).collect(Collectors.toList());
            for (ApiConsumerImpact impact : relatedImpacts) {
                ConsumerReport consumer = ConsumerReport.builder()
                    .fileName(impact.getFileName())
                    .className(impact.getClassName())
                    .methodName(impact.getMethodName())
                    .confidence(impact.getConfidence())
                    .evidence(impact.getEvidence())
                    .build();
                    
                // Generate path from graph (simplified placeholder logic)
                consumer.getDependencyPath().add(impact.getClassName() + "." + impact.getMethodName());
                cr.getImpactedConsumers().add(consumer);
            }
            
            report.getChanges().add(cr);
        }
        return report;
    }
}
