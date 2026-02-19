/*
 * Copyright 2021-2026 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.openaire.observatory.datasets;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
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

@Configuration
@EntityScan(basePackages = "eu.openaire.observatory.datasets")
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef = "datasetsEntityManagerFactory",
        transactionManagerRef = "datasetsTransactionManager",
        basePackages = {"eu.openaire.observatory.datasets"})
public class DatasourceConfig {

    @Bean(name = "datasetsJpaProperties")
    @ConfigurationProperties("datasets.jpa")
    public JpaProperties datasetsJpaProperties() {
        return new JpaProperties();
    }

    @Bean(name = "datasetsDataSourceProperties")
    @ConfigurationProperties("datasets.datasource")
    public DataSourceProperties datasetsDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "datasetsDataSource")
    @ConditionalOnMissingBean(name = "datasetsDataSource")
    @ConfigurationProperties("datasets.datasource.configuration")
    public DataSource datasetsDataSource(
            @Qualifier("datasetsDataSourceProperties") DataSourceProperties datasetsDataSourceProperties) {
        return datasetsDataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "datasetsEntityManagerFactory")
    @ConditionalOnMissingBean(name = "datasetsEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean datasetsEntityManagerFactory(
            EntityManagerFactoryBuilder datasetsEntityManagerFactoryBuilder,
            @Qualifier("datasetsDataSource") DataSource datasetsDataSource,
            @Qualifier("datasetsJpaProperties") JpaProperties datasetsJpaProperties) {

        return datasetsEntityManagerFactoryBuilder
                .dataSource(datasetsDataSource)
                .packages("eu.openaire.observatory.datasets")
                .persistenceUnit("datasetsDataSource")
                .properties(datasetsJpaProperties.getProperties())
                .build();
    }

    @Bean(name = "datasetsTransactionManager")
    @ConditionalOnMissingBean(name = "datasetsTransactionManager")
    public PlatformTransactionManager datasetsTransactionManager(
            @Qualifier("datasetsEntityManagerFactory") EntityManagerFactory datasetsEntityManagerFactory) {
        return new JpaTransactionManager(datasetsEntityManagerFactory);
    }

}
