package eu.eosc.observatory.configuration;

import gr.athenarc.catalogue.CatalogueApplication;
import gr.athenarc.catalogue.config.RegistryCoreConfiguration;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import java.util.HashMap;

@Configuration
@ComponentScan(value = {
        "gr.athenarc.catalogue",
        "eu.openminted.registry.core",
},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = CatalogueApplication.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = RegistryCoreConfiguration.class)
        })
public class ObservatoryConfiguration {

        @Bean
        public EntityManagerFactoryBuilder entityManagerFactoryBuilder() {
                return new EntityManagerFactoryBuilder(new HibernateJpaVendorAdapter(), new HashMap<>(), null);
        }
}
