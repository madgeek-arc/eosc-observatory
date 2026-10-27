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


package eu.openaire.observatory.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SimpleCORSFilterTest {

    private static final String FRONTEND = "https://observatory.example.org";

    private MockHttpServletResponse filter(Set<String> allowedOrigins, String origin) throws Exception {
        ApplicationProperties properties = new ApplicationProperties();
        properties.setCorsAllowedOrigins(allowedOrigins);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stakeholders");
        if (origin != null) {
            request.addHeader("Origin", origin);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        new SimpleCORSFilter(properties).doFilter(request, response, chain);
        assertNotNull(chain.getRequest());
        return response;
    }

    @Test
    void allowsConfiguredOriginWithCredentials() throws Exception {
        MockHttpServletResponse response = filter(Set.of(FRONTEND), FRONTEND);

        assertEquals(FRONTEND, response.getHeader("Access-Control-Allow-Origin"));
        assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
        assertEquals("Origin", response.getHeader("Vary"));
    }

    @Test
    void ignoresOtherOrigins() throws Exception {
        MockHttpServletResponse response = filter(Set.of(FRONTEND), "https://attacker.example");

        assertNull(response.getHeader("Access-Control-Allow-Origin"));
        assertNull(response.getHeader("Access-Control-Allow-Credentials"));
    }

    @Test
    void sendsNoCorsHeadersByDefault() throws Exception {
        MockHttpServletResponse response = filter(new ApplicationProperties().getCorsAllowedOrigins(), FRONTEND);

        assertNull(response.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    void passesRequestsWithoutOrigin() throws Exception {
        assertNull(filter(Set.of(FRONTEND), null).getHeader("Access-Control-Allow-Origin"));
    }
}
