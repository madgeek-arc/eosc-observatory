/**
 * Copyright 2021-2025 OpenAIRE AMKE
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
package eu.openaire.observatory.commenting;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EntityScan(basePackages = "eu.openaire.observatory.commenting.domain")
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef = "commentingEntityManagerFactory",
        transactionManagerRef = "commentingTransactionManager",
        basePackages = {"eu.openaire.observatory.commenting.repository"})
@EnableJpaAuditing(auditorAwareRef = "securityAuditorAware")
public class CommentingDatasourceConfig {

    @Bean(name = "commentingJpaProperties")
    @ConfigurationProperties("commenting.jpa")
    public JpaProperties commentingJpaProperties() {
        return new JpaProperties();
    }

    @Bean(name = "commentingDataSourceProperties")
    @ConfigurationProperties("commenting.datasource")
    public DataSourceProperties commentingDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "commentingDataSource")
    @ConditionalOnMissingBean(name = "commentingDataSource")
    @ConfigurationProperties("commenting.datasource.configuration")
    public DataSource commentingDataSource(
            @Qualifier("commentingDataSourceProperties") DataSourceProperties commentingDataSourceProperties) {
        return commentingDataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "commentingEntityManagerFactory")
    @ConditionalOnMissingBean(name = "commentingEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean commentingEntityManagerFactory(
            EntityManagerFactoryBuilder commentingEntityManagerFactoryBuilder,
            @Qualifier("commentingDataSource") DataSource commentingDataSource,
            @Qualifier("commentingJpaProperties") JpaProperties commentingJpaProperties) {

        return commentingEntityManagerFactoryBuilder
                .dataSource(commentingDataSource)
                .packages("eu.openaire.observatory.commenting")
                .persistenceUnit("commentingDataSource")
                .properties(commentingJpaProperties.getProperties())
                .build();
    }

    @Bean(name = "commentingTransactionManager")
    @ConditionalOnMissingBean(name = "commentingTransactionManager")
    public PlatformTransactionManager commentingTransactionManager(
            @Qualifier("commentingEntityManagerFactory") EntityManagerFactory commentingEntityManagerFactory) {
        return new JpaTransactionManager(commentingEntityManagerFactory);
    }

}