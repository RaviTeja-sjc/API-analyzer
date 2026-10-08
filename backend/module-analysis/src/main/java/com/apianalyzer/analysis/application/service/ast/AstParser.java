package com.apianalyzer.analysis.application.service.ast;

import java.util.List;

/**
 * Strategy interface for parsing Abstract Syntax Trees across different languages.
 * Implementations should support extracting classes, methods, and specific API call patterns
 * for a targeted programming language (e.g., Java, TypeScript, Python).
 */
public interface AstParser {
    
    /**
     * Parses the AST to find all methods that invoke a specific API endpoint.
     * 
     * @param rootDirectory The root directory of the source code repository to scan.
     * @param apiEndpoint   The API endpoint path (e.g., "/api/v1/users").
     * @return A list of AST nodes or fully-qualified method signatures that consume the endpoint.
     */
    List<String> findConsumersOfEndpoint(String rootDirectory, String apiEndpoint);
    
    /**
     * Applies a migration patch to a specific consumer file.
     * 
     * @param filePath         The absolute path to the file.
     * @param targetLiteral    The exact string literal to replace.
     * @param replacementValue The new string literal value.
     */
    void applyMigrationPatch(String filePath, String targetLiteral, String replacementValue);
    
    /**
     * @return The language supported by this parser (e.g., "JAVA", "TYPESCRIPT", "PYTHON").
     */
    String getSupportedLanguage();
}
