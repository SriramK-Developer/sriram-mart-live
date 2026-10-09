package com.srirammart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SriramMartApplication {
    public static void main(String[] args) {
        SpringApplication.run(SriramMartApplication.class, args);
    }
}
