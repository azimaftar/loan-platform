package com.azimali.loanplatform.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Loan Platform API")
                        .description("""
                    Cloud-Native Credit Risk & Loan Decisioning Platform
                    
                    ## Features
                    - JWT Authentication with role-based access control
                    - Loan lifecycle management (PENDING → UNDER_REVIEW → APPROVED/REJECTED → PAID)
                    - Automated risk scoring engine
                    - Automated decision engine with configurable thresholds
                    - Full audit trail for all actions
                    
                    ## Roles
                    - **USER** — can apply for loans, view own loans, mark loans as paid
                    - **LOAN_OFFICER** — can manually approve or reject loans
                    - **ADMIN** — full access to all endpoints
                    """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Azim Ali")
                                .email("azim@example.com")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
