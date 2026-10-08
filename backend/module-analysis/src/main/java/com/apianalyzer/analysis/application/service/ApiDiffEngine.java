package com.apianalyzer.analysis.application.service;
import com.apianalyzer.analysis.application.service.rules.DiffContext;
import com.apianalyzer.analysis.application.service.rules.DiffRule;
import com.apianalyzer.analysis.domain.model.NormalizedApiModel.Api;
import com.apianalyzer.analysis.domain.model.diff.ApiChange;

import org.springframework.stereotype.Service;
import java.util.List;
@Service

public class ApiDiffEngine {
    private final List<DiffRule> rules;
    
    public ApiDiffEngine(List<DiffRule> rules) {
        this.rules = rules;
    }
    public List<ApiChange> compare(Api base, Api head) {
        DiffContext context = new DiffContext(base, head);
        for (DiffRule rule : rules) {
            rule.evaluate(context);
        }
        return context.getChanges();
    }
}
