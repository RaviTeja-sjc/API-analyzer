package com.apianalyzer.analysis.application.service.rules;
import com.apianalyzer.analysis.domain.model.NormalizedApiModel.Endpoint;
import com.apianalyzer.analysis.domain.model.NormalizedApiModel.Parameter;
import com.apianalyzer.analysis.domain.model.diff.ApiChange;
import com.apianalyzer.analysis.domain.model.diff.ChangeSeverity;
import com.apianalyzer.analysis.domain.model.diff.ChangeType;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.stream.Collectors;
@Component
public class ParameterRule implements DiffRule {
    @Override public String getName() { return "Parameter Rule"; }
    @Override
    public void evaluate(DiffContext context) {
        Map<String, Endpoint> headMap = context.getHead().getEndpoints().stream()
                .collect(Collectors.toMap(e -> e.getMethod() + " " + e.getPath(), e -> e));
                
        for (Endpoint baseEp : context.getBase().getEndpoints()) {
            String key = baseEp.getMethod() + " " + baseEp.getPath();
            Endpoint headEp = headMap.get(key);
            if (headEp == null) continue; // Handled by EndpointRemovedRule
            
            Map<String, Parameter> headParams = headEp.getParameters().stream()
                    .collect(Collectors.toMap(p -> p.getName() + "|" + p.getIn(), p -> p));
                    
            for (Parameter baseP : baseEp.getParameters()) {
                String pKey = baseP.getName() + "|" + baseP.getIn();
                if (!headParams.containsKey(pKey)) {
                    context.addChange(ApiChange.builder().type(ChangeType.PARAMETER_REMOVED).severity(ChangeSeverity.BREAKING)
                        .description("Parameter removed: " + baseP.getName()).path(baseEp.getPath()).method(baseEp.getMethod())
                        .oldValue("Present").newValue("Removed").build());
                } else {
                    Parameter headP = headParams.get(pKey);
                    if (!baseP.isRequired() && headP.isRequired()) {
                        context.addChange(ApiChange.builder().type(ChangeType.PARAMETER_CHANGED).severity(ChangeSeverity.BREAKING)
                            .description("Parameter became required: " + headP.getName()).path(baseEp.getPath()).method(baseEp.getMethod())
                            .oldValue("Optional").newValue("Required").build());
                    }
                }
            }
        }
    }
}
