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

package eu.openaire.observatory.erasure;

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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EntityScan(basePackages = "eu.openaire.observatory.erasure.domain")
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef = "erasureEntityManagerFactory",
        transactionManagerRef = "erasureTransactionManager",
        basePackages = {"eu.openaire.observatory.erasure.repository"})
public class ErasureDatasourceConfig {

    @Bean(name = "erasureJpaProperties")
    @ConfigurationProperties("erasure.jpa")
    public JpaProperties erasureJpaProperties() {
        return new JpaProperties();
    }

    @Bean(name = "erasureDataSourceProperties")
    @ConfigurationProperties("erasure.datasource")
    public DataSourceProperties erasureDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "erasureDataSource")
    @ConditionalOnMissingBean(name = "erasureDataSource")
    @ConfigurationProperties("erasure.datasource.configuration")
    public DataSource erasureDataSource(
            @Qualifier("erasureDataSourceProperties") DataSourceProperties erasureDataSourceProperties) {
        return erasureDataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "erasureEntityManagerFactory")
    @ConditionalOnMissingBean(name = "erasureEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean erasureEntityManagerFactory(
            EntityManagerFactoryBuilder erasureEntityManagerFactoryBuilder,
            @Qualifier("erasureDataSource") DataSource erasureDataSource,
            @Qualifier("erasureJpaProperties") JpaProperties erasureJpaProperties) {

        return erasureEntityManagerFactoryBuilder
                .dataSource(erasureDataSource)
                .packages("eu.openaire.observatory.erasure")
                .persistenceUnit("erasureDataSource")
                .properties(erasureJpaProperties.getProperties())
                .build();
    }

    @Bean(name = "erasureTransactionManager")
    @ConditionalOnMissingBean(name = "erasureTransactionManager")
    public PlatformTransactionManager erasureTransactionManager(
            @Qualifier("erasureEntityManagerFactory") EntityManagerFactory erasureEntityManagerFactory) {
        return new JpaTransactionManager(erasureEntityManagerFactory);
    }

}
