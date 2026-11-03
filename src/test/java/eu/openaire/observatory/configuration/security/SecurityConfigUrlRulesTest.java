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


package eu.openaire.observatory.configuration.security;

import eu.openaire.observatory.configuration.ApplicationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(SecurityConfigUrlRulesTest.Config.class)
class SecurityConfigUrlRulesTest {

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, PathsController.class})
    static class Config {

        @Bean
        ApplicationProperties applicationProperties() {
            ApplicationProperties properties = new ApplicationProperties();
            properties.setLogoutRedirect("http://localhost:4200");
            return properties;
        }

        @Bean
        AuthenticationSuccessHandler authenticationSuccessHandler() {
            return (request, response, authentication) -> {
            };
        }

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("test")
                    .clientId("client")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .authorizationUri("http://idp/authorize")
                    .tokenUri("http://idp/token")
                    .build());
        }
    }

    @RestController
    static class PathsController {

        @RequestMapping({"/version", "/version/**", "/schemaService/**", "/resourcesync/**",
                "/request/default", "/ui/vocabularies/map", "/stakeholders"})
        String ok() {
            return "ok";
        }
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void registryVersionSchemaAndResourceSyncPathsRequireAdmin() throws Exception {
        for (String path : Set.of("/version", "/version/user/u1", "/schemaService/s1", "/resourcesync/")) {
            mvc.perform(get(path)).andExpect(status().is3xxRedirection());
            mvc.perform(get(path).with(user("user@example.org"))).andExpect(status().isForbidden());
            mvc.perform(get(path).with(user("admin@example.org").authorities(new SimpleGrantedAuthority("ADMIN"))))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void catalogueDataRequestRequiresLogin() throws Exception {
        mvc.perform(post("/request/default").with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/request/default").with(csrf()).with(user("user@example.org")))
                .andExpect(status().isOk());
    }

    @Test
    void otherPathsStayPublic() throws Exception {
        mvc.perform(get("/ui/vocabularies/map")).andExpect(status().isOk());
        mvc.perform(get("/stakeholders")).andExpect(status().isOk());
    }
}
