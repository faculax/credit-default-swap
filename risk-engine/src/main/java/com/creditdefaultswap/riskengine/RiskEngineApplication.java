package com.creditdefaultswap.riskengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class RiskEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiskEngineApplication.class, args);
    }

}
