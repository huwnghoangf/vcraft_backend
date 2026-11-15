package com.vcraft.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vcraft.domain.model.User;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonConfig {
    abstract static class SafeUser {
        @JsonIgnore abstract String passwordHash();
    }
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer safeUsers() {
        return builder -> builder.mixIn(User.class, SafeUser.class);
    }
}
