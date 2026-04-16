package com.whispernetwork.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the API orchestration service.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ApiServiceApplication {

    /**
     * Starts the API service.
     *
     * @param args command line args
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiServiceApplication.class, args);
    }
}
