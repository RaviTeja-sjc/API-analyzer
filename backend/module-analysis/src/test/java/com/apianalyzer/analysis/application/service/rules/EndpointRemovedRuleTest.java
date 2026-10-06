package com.apianalyzer.analysis.application.service.rules;
import com.apianalyzer.analysis.domain.model.NormalizedApiModel.*;
import com.apianalyzer.analysis.domain.model.diff.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class EndpointRemovedRuleTest {

    @Test
    void testEndpointRemovedIsBreaking() {
        EndpointRemovedRule rule = new EndpointRemovedRule();
        
        Api baseApi = Api.builder().endpoints(List.of(
                Endpoint.builder().method("GET").path("/users").build()
        )).build();
        
        Api headApi = Api.builder().endpoints(List.of()).build(); // Endpoint removed
        
        DiffContext context = new DiffContext(baseApi, headApi);
        rule.evaluate(context);
        
        assertEquals(1, context.getChanges().size());
        ApiChange change = context.getChanges().get(0);
        assertEquals(ChangeType.ENDPOINT_REMOVED, change.getType());
        assertEquals(ChangeSeverity.BREAKING, change.getSeverity());
        assertEquals("GET", change.getMethod());
        assertEquals("/users", change.getPath());
    }
    
    @Test
    void testEndpointKeptNoChange() {
        EndpointRemovedRule rule = new EndpointRemovedRule();
        
        Api baseApi = Api.builder().endpoints(List.of(Endpoint.builder().method("GET").path("/users").build())).build();
        Api headApi = Api.builder().endpoints(List.of(Endpoint.builder().method("GET").path("/users").build())).build();
        
        DiffContext context = new DiffContext(baseApi, headApi);
        rule.evaluate(context);
        
        assertTrue(context.getChanges().isEmpty());
    }
}
