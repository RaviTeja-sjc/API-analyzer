package com.apianalyzer.analysis.application.service.rules;
import com.apianalyzer.analysis.domain.model.NormalizedApiModel.Api;
import com.apianalyzer.analysis.domain.model.diff.ApiChange;
import lombok.Getter;
import java.util.ArrayList;
import java.util.List;
@Getter
public class DiffContext {
    private final Api base;
    private final Api head;
    private final List<ApiChange> changes = new ArrayList<>();
    
    public DiffContext(Api base, Api head) {
        this.base = base;
        this.head = head;
    }
    public void addChange(ApiChange change) { this.changes.add(change); }
}
