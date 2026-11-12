
package ru.constructor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI constructorKvOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ConstructorKV API")
                        .version("1.0.0")
                        .description("""
                                REST API системы формирования и управления комплектовками.

                                Система обеспечивает:
                                - просмотр дерева спецификаций;
                                - работу со служебными записками;
                                - формирование комплектовок;
                                - просмотр и управление структурой комплектовок;
                                - контроль серийности изделий.
                                """));
    }
}
