package eu.eosc.observatory.configuration;


import gr.athenarc.authorization.AuthorizationApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ComponentScan(basePackages = {
        "gr.athenarc.authorization",
},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = AuthorizationApplication.class)
        })
@EntityScan(basePackages = "gr.athenarc.authorization.domain")
@EnableJpaRepositories(basePackages = "gr.athenarc.authorization.repository")
@EnableTransactionManagement
public class AuthorizationConfiguration {
}
