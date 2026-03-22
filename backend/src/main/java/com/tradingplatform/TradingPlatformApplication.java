package com.tradingplatform;

import com.tradingplatform.config.JwtConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableConfigurationProperties(JwtConfig.class)
public class TradingPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingPlatformApplication.class, args);
    }
}
