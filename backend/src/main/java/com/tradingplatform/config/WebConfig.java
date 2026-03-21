package com.tradingplatform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${avatar.upload.dir:./uploads/avatars}")
    private String uploadDir;

    /**
     * Configures static resource handling for avatar uploads.
     * Allows serving avatar files from /uploads/avatars/** URL path.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Ensure the path ends with a slash for proper file URL resolution
        String location = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        String resourceLocation = "file:" + location;

        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(resourceLocation);
    }
}