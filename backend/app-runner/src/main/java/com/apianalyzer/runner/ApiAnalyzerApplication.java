package com.apianalyzer.runner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.apianalyzer")
@EnableJpaRepositories(basePackages = "com.apianalyzer")
@EntityScan(basePackages = "com.apianalyzer")
public class ApiAnalyzerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiAnalyzerApplication.class, args);
    }
}
