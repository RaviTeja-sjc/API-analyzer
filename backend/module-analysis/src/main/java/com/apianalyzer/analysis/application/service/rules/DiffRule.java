package com.apianalyzer.analysis.application.service.rules;
public interface DiffRule {
    void evaluate(DiffContext context);
    String getName();
}
