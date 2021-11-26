package eu.eosc.observatory.configuration;


import gr.athenarc.authorization.AuthorizationApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;

@Configuration
@ComponentScan(basePackages = {
        "gr.athenarc.authorization",
},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = AuthorizationApplication.class)
        })
@EntityScan(basePackages = "gr.athenarc.authorization.domain")
//@EnableJpaRepositories(basePackages = "gr.athenarc.authorization.repository")
@EnableJpaRepositories(
        basePackages = "gr.athenarc.authorization.repository",
        entityManagerFactoryRef = "authEntityManager",
        transactionManagerRef = "authTransactionManager")
@EnableTransactionManagement
public class AuthorizationConfiguration {

        @Autowired
        private Environment env;

        @Bean
        @ConfigurationProperties(prefix="spring.datasource")
        public DataSource authDataSource() {
                return DataSourceBuilder.create().build();
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean authEntityManager() {
                LocalContainerEntityManagerFactoryBean em
                        = new LocalContainerEntityManagerFactoryBean();
                em.setDataSource(authDataSource());
                em.setPackagesToScan(
                        new String[] { "gr.athenarc.authorization.domain" });

                HibernateJpaVendorAdapter vendorAdapter
                        = new HibernateJpaVendorAdapter();
                em.setJpaVendorAdapter(vendorAdapter);
                HashMap<String, Object> properties = new HashMap<>();
                properties.put("hibernate.hbm2ddl.auto",
                        env.getProperty("hibernate.hbm2ddl.auto"));
                properties.put("hibernate.dialect",
                        env.getProperty("spring.jpa.properties.hibernate.dialect"));
                em.setJpaPropertyMap(properties);

                return em;
        }

        @Bean
        public PlatformTransactionManager authTransactionManager() {

                JpaTransactionManager transactionManager
                        = new JpaTransactionManager();
                transactionManager.setEntityManagerFactory(
                        authEntityManager().getObject());
                return transactionManager;
        }
}
