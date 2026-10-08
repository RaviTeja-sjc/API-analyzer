package com.apianalyzer.analysis.application.service.rules.project;

import com.apianalyzer.analysis.application.service.ast.JavaAstParserService;
import com.apianalyzer.analysis.domain.model.repository.RepositoryProvider;
import com.apianalyzer.analysis.domain.model.repository.RepositorySnapshot;
import com.apianalyzer.core.domain.entity.Project;
import com.apianalyzer.core.domain.entity.ProjectIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ProjectAnalysisRulesTest {

    private HardcodedSecretRule secretRule;
    private InappropriateLayerCouplingRule couplingRule;
    private ProjectAnalysisEngine engine;

    @BeforeEach
    void setUp() {
        secretRule = new HardcodedSecretRule();
        couplingRule = new InappropriateLayerCouplingRule(new JavaAstParserService());
        engine = new ProjectAnalysisEngine(List.of(secretRule, couplingRule));
    }

    private ProjectAnalysisContext buildContext(String fileName, String fileContent) {
        RepositorySnapshot.RepositoryFile file = RepositorySnapshot.RepositoryFile.builder()
                .path(fileName)
                .type("blob")
                .size(100)
                .content(null)
                .build();
        
        RepositorySnapshot snapshot = RepositorySnapshot.builder()
                .url("https://github.com/test/repo")
                .headCommitSha("main")
                .files(List.of(file))
                .build();

        RepositoryProvider dummyProvider = new RepositoryProvider() {
            @Override
            public RepositorySnapshot fetchSnapshot(String url, String commitSha) {
                return snapshot;
            }
            @Override
            public String fetchFileContent(String url, String commitSha, String path) {
                return path.equals(fileName) ? fileContent : null;
            }
        };

        return ProjectAnalysisContext.builder()
                .projectId(UUID.randomUUID())
                .analysisJobId(UUID.randomUUID())
                .project(new Project())
                .snapshot(snapshot)
                .repositoryProvider(dummyProvider)
                .build();
    }

    @Test
    void test1_HardcodedSecretDetected() {
        ProjectAnalysisContext context = buildContext("Config.java", "String password = \"super_secret_123\";");
        List<ProjectIssue> issues = engine.runAnalysis(context);
        
        List<ProjectIssue> secretIssues = issues.stream()
                .filter(i -> i.getRuleId().equals("SEC-HARDCODED-SECRET"))
                .collect(Collectors.toList());
                
        assertEquals(1, secretIssues.size());
        ProjectIssue issue = secretIssues.get(0);
        assertTrue(issue.getEvidence().contains("[REDACTED]"));
        assertFalse(issue.getEvidence().contains("super_secret_123"));
        assertEquals("Config.java", issue.getFilePath());
    }

    @Test
    void test2_HardcodedSecretRemoved() {
        ProjectAnalysisContext context = buildContext("Config.java", "String password = System.getenv(\"DB_PASS\");");
        List<ProjectIssue> issues = engine.runAnalysis(context);
        
        long secretCount = issues.stream().filter(i -> i.getRuleId().equals("SEC-HARDCODED-SECRET")).count();
        assertEquals(0, secretCount);
    }

    @Test
    void test3_UnrelatedStrings() {
        ProjectAnalysisContext context = buildContext("Config.java", "String welcomeMessage = \"Hello World!\";");
        List<ProjectIssue> issues = engine.runAnalysis(context);
        
        long secretCount = issues.stream().filter(i -> i.getRuleId().equals("SEC-HARDCODED-SECRET")).count();
        assertEquals(0, secretCount);
    }

    @Test
    void test4_JavaRepositoryAnalyzed_LayerCoupling() {
        String javaCode = "package com.example; " +
                "@org.springframework.web.bind.annotation.RestController " +
                "class MyController { " +
                "  private UserRepository userRepository; " +
                "}";
        ProjectAnalysisContext context = buildContext("MyController.java", javaCode);
        List<ProjectIssue> issues = engine.runAnalysis(context);
        
        List<ProjectIssue> archIssues = issues.stream()
                .filter(i -> i.getRuleId().equals("ARCH-LAYER-COUPLING"))
                .collect(Collectors.toList());
                
        assertEquals(1, archIssues.size());
        assertEquals("MyController.java", archIssues.get(0).getFilePath());
    }

    @Test
    void test5_NonJavaRepository_NoFabrications() {
        String pythonCode = "import requests\n" +
                "class MyController:\n" +
                "    userRepository = None"; // resembles java names but is python
        ProjectAnalysisContext context = buildContext("main.py", pythonCode);
        List<ProjectIssue> issues = engine.runAnalysis(context);
        
        // Java AST parser won't parse python or coupling rule ignores it
        long archCount = issues.stream().filter(i -> i.getRuleId().equals("ARCH-LAYER-COUPLING")).count();
        assertEquals(0, archCount);
    }

    @Test
    void test7_MultipleRepositoriesDiffer() {
        ProjectAnalysisContext ctxA = buildContext("A.java", "password=\"abc\";");
        ProjectAnalysisContext ctxB = buildContext("B.java", "System.out.println(\"ok\");");
        
        List<ProjectIssue> issuesA = engine.runAnalysis(ctxA);
        List<ProjectIssue> issuesB = engine.runAnalysis(ctxB);
        
        assertEquals(1, issuesA.size());
        assertEquals(0, issuesB.size());
    }
}
