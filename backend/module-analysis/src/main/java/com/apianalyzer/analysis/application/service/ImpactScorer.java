package com.apianalyzer.analysis.application.service;
import com.apianalyzer.analysis.domain.model.diff.ApiChange;
import com.apianalyzer.analysis.domain.model.diff.ChangeSeverity;
import com.apianalyzer.analysis.domain.model.diff.ChangeType;
import com.apianalyzer.analysis.domain.model.diff.ImpactClassification;
import com.apianalyzer.analysis.domain.model.diff.ImpactClassification.Level;
import org.springframework.stereotype.Service;
@Service
public class ImpactScorer {
    
    public ImpactClassification evaluate(ApiChange change, int activeConsumers, boolean isCoreEndpoint) {
        ImpactClassification impact = ImpactClassification.builder().score(0).confidence(0.9).build();
        
        // 1. Base score from breaking status
        if (change.getSeverity() == ChangeSeverity.BREAKING) {
            impact.setScore(impact.getScore() + 50);
            impact.addReason("Base +50: Change is strictly BREAKING.");
        } else if (change.getSeverity() == ChangeSeverity.POTENTIALLY_BREAKING) {
            impact.setScore(impact.getScore() + 25);
            impact.addReason("Base +25: Change is POTENTIALLY BREAKING.");
        } else {
            impact.addReason("Base +0: Change is NON-BREAKING backwards compatible.");
        }
        
        // 2. Authentication impact
        if (change.getType() == ChangeType.SECURITY_CHANGED) {
            impact.setScore(impact.getScore() + 30);
            impact.addReason("Auth +30: Modifications to endpoint security/auth usually break existing integrations.");
        }
        
        // 3. Endpoint Importance
        if (isCoreEndpoint) {
            impact.setScore(impact.getScore() + 20);
            impact.addReason("Importance +20: Path is flagged as a core critical endpoint.");
        }
        
        // 4. Affected Consumers Multiplier
        if (activeConsumers > 100) {
            impact.setScore(impact.getScore() + 30);
            impact.addReason("Consumers +30: High usage detected (" + activeConsumers + " known consumers).");
        } else if (activeConsumers > 10) {
            impact.setScore(impact.getScore() + 15);
            impact.addReason("Consumers +15: Moderate usage detected (" + activeConsumers + " known consumers).");
        } else if (activeConsumers == 0 && change.getSeverity() == ChangeSeverity.BREAKING) {
            // Deduct severity if no one is using it
            impact.setScore(Math.max(0, impact.getScore() - 40));
            impact.addReason("Consumers -40: Breaking change, but 0 known active consumers affected.");
            impact.setConfidence(0.7); // Lower confidence because unknown consumers might exist
        }
        
        // Cap score
        impact.setScore(Math.min(100, Math.max(0, impact.getScore())));
        
        // Assign Level
        if (impact.getScore() >= 80) impact.setLevel(Level.CRITICAL);
        else if (impact.getScore() >= 50) impact.setLevel(Level.HIGH);
        else if (impact.getScore() >= 20) impact.setLevel(Level.MEDIUM);
        else impact.setLevel(Level.LOW);
        
        return impact;
    }
}
