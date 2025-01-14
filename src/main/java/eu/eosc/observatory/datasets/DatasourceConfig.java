package eu.eosc.observatory.datasets;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EntityScan(basePackages = "eu.eosc.observatory.datasets")
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef = "datasetsEntityManagerFactory",
        transactionManagerRef = "datasetsTransactionManager",
        basePackages = {"eu.eosc.observatory.datasets"})
public class DatasourceConfig {

    @Bean
    @ConfigurationProperties("datasets.datasource")
    public DataSourceProperties datasetsDatasourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("datasets.jpa.properties")
    public Map<String, String> datasetsProperties() {
        return new HashMap<>();
    }

    @Bean(name = "datasetsDataSource")
    @ConditionalOnMissingBean(name = "datasetsDataSource")
    public DataSource datasetsDataSource() {
        return datasetsDatasourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "datasetsEntityManagerFactory")
    @ConditionalOnMissingBean(name = "datasetsEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean datasetsEntityManagerFactory(
            EntityManagerFactoryBuilder datasetsEntityManagerFactoryBuilder, @Qualifier("datasetsDataSource") DataSource datasetsDataSource, Map<String, String> datasetProperties) {

        return datasetsEntityManagerFactoryBuilder
                .dataSource(datasetsDataSource)
                .packages("eu.eosc.observatory.datasets")
                .persistenceUnit("datasetsDataSource")
                .properties(datasetProperties)
                .build();
    }

    @Bean(name = "datasetsTransactionManager")
    @ConditionalOnMissingBean(name = "datasetsTransactionManager")
    public PlatformTransactionManager datasetsTransactionManager(
            @Qualifier("datasetsEntityManagerFactory") EntityManagerFactory datasetsEntityManagerFactory) {
        return new JpaTransactionManager(datasetsEntityManagerFactory);
    }

}
