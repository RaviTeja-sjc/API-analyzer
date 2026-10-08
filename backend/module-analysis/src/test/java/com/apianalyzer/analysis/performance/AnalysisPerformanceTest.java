package com.apianalyzer.analysis.performance;

import com.apianalyzer.analysis.application.service.ast.JavaAstParserService;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AnalysisPerformanceTest {

    @Test
    void testAstParsingAtScale() {
        JavaAstParserService service = new JavaAstParserService();
        
        // Generate a massive repository (1,000 files, 100 methods each)
        List<String> simulatedRepoFiles = IntStream.range(0, 1000)
            .mapToObj(i -> {
                StringBuilder builder = new StringBuilder();
                builder.append("package com.example;\n");
                builder.append("public class MassiveService").append(i).append(" {\n");
                for (int j = 0; j < 100; j++) {
                    builder.append("    public void doWork").append(j).append("() { }\n");
                }
                builder.append("}\n");
                return builder.toString();
            })
            .collect(Collectors.toList());

        long startTime = System.currentTimeMillis();
        
        // Optimize: Use parallel streams for memory-conscious AST parsing
        simulatedRepoFiles.parallelStream().forEach(file -> {
            service.parseSourceCode(file, "File.java");
        });
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("Parsed 1,000 files (100,000 methods total) in {} ms", duration);
        
        // Ensure parsing 1,000 massive ASTs takes less than 5 seconds in parallel
        assertTrue(duration < 5000, "AST parsing is too slow! Took: " + duration + "ms");
    }
}
