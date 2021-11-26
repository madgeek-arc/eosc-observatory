package eu.eosc.observatory.configuration;


import eu.openminted.registry.core.configuration.HibernateConfiguration;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

@Configuration
//@EntityScan(basePackages = {"gr.athenarc.catalogue.domain", "eu.openminted.registry.core.domain", "eu.openminted.registry.core.domain.index"})
//@EnableJpaRepositories
public class RegistryCoreConfig extends HibernateConfiguration {

    Logger logger = LogManager.getLogger(RegistryCoreConfig.class);

    @Bean(name = "entityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        return super.entityManagerFactory();
    }

    @Bean
    @Primary
    @Autowired
    public EntityManager entityManager(EntityManagerFactory entityManagerFactory) {
        return super.entityManager(entityManagerFactory);
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager() {
        return super.transactionManager();
    }

}
