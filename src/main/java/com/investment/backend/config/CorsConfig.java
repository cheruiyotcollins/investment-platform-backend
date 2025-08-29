package com.investment.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(
                                "https://www.cryptoinvestment.live",
                                "https://api.cryptoinvestment.live", // fixed typo: was api/cryptoinvestment.live
                                "https://cryptoinvest-live.netlify.app",
                                "http://localhost:3000",
                                "https://likely-rhianon-cryptoinvest-ba86ef09.koyeb.app/" // add Koyeb domain
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
