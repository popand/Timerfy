package com.timerfy.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Value("${server.port:3001}")
    private String serverPort;
    
    @Bean
    public OpenAPI timerfyOpenAPI() {
        Server localServer = new Server()
                .url("http://localhost:" + serverPort)
                .description("Local development server");
        
        Contact contact = new Contact()
                .name("Timerfy Development Team")
                .email("dev@timerfy.io")
                .url("https://github.com/timerfy/timerfy-server");
        
        License license = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");
        
        Info info = new Info()
                .title("Timerfy Server API")
                .description("Distributed Timer System - RESTful API for managing timer rooms, timers, and messages")
                .version("1.0.0")
                .contact(contact)
                .license(license)
                .summary("A free, web-based distributed countdown timer system with real-time synchronization");
        
        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}