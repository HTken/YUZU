package com.yuzu.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SuscripcionInterceptor suscripcionInterceptor;

    public WebConfig(SuscripcionInterceptor suscripcionInterceptor) {
        this.suscripcionInterceptor = suscripcionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Un restaurante suspendido sí puede iniciar sesión y ver su plan, para poder renovarlo.
        // La consola de plataforma no depende de ningún restaurante.
        // Un restaurante suspendido también puede pagar, que es justo lo que lo reactiva.
        registry.addInterceptor(suscripcionInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/plataforma/**", "/api/pagos/**", "/api/mi-plan", "/api/me");
    }
}
