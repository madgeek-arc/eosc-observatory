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


package eu.openaire.observatory.messaging;

import com.sun.net.httpserver.HttpServer;
import eu.openaire.observatory.configuration.security.MethodSecurityExpressionsService;
import eu.openaire.observatory.utils.OidcTestUtils;
import gr.athenarc.messaging.config.MessagingClientProperties;
import gr.athenarc.messaging.domain.Correspondent;
import gr.athenarc.messaging.domain.TopicThread;
import gr.athenarc.messaging.dto.ThreadDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessagingSystemControllerTest {

    private static final String USER = "user@example.org";
    private static final String THREAD_JSON = "{\"id\":\"t1\",\"subject\":\"s\"}";

    private HttpServer server;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    /** Group id for which the stubbed service finds thread t1; any other lookup returns no thread. */
    private volatile String participantGroup;
    private AnnotationConfigApplicationContext context;
    private MessagingSystemController controller;
    private MessagingService messagingService;

    @Configuration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class Config {
    }

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            String request = exchange.getRequestMethod() + " " + exchange.getRequestURI();
            requests.add(request);
            byte[] body = new byte[0];
            boolean lookup = exchange.getRequestMethod().equals("GET");
            if (!lookup || (participantGroup != null && request.contains("groupId=" + participantGroup))) {
                body = THREAD_JSON.getBytes(StandardCharsets.UTF_8);
            }
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length == 0 ? -1 : body.length);
            if (body.length > 0) {
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();

        MessagingClientProperties properties = new MessagingClientProperties();
        properties.getClient().setEndpoint("http://localhost:" + server.getAddress().getPort() + "/");
        messagingService = mock(MessagingService.class);
        MethodSecurityExpressionsService expressions = mock(MethodSecurityExpressionsService.class);
        when(expressions.userIsMemberOfGroup(eq(USER), eq("g1"))).thenReturn(true);

        context = new AnnotationConfigApplicationContext();
        context.register(Config.class);
        context.registerBean("methodSecurityExpressionsService", MethodSecurityExpressionsService.class, () -> expressions);
        context.registerBean(MessagingSystemController.class, () -> new MessagingSystemController(properties,
                context.getBeanProvider(WebClient.Builder.class), messagingService, mock(EmailOperations.class)));
        context.refresh();
        controller = context.getBean(MessagingSystemController.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
        server.stop(0);
    }

    private void authenticate(String email, String... authorities) {
        UsernamePasswordAuthenticationToken oidc = OidcTestUtils.oidcAuthentication(email);
        List<GrantedAuthority> granted = new ArrayList<>(oidc.getAuthorities());
        for (String authority : authorities) {
            granted.add(new SimpleGrantedAuthority(authority));
        }
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(oidc.getPrincipal(), "token", granted);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private boolean sent(String methodAndPathPrefix) {
        return requests.stream().anyMatch(r -> r.startsWith(methodAndPathPrefix));
    }

    private static ThreadDTO thread(String fromEmail, String fromGroup) {
        ThreadDTO thread = new ThreadDTO();
        thread.setFrom(new Correspondent("User", fromEmail, fromGroup));
        return thread;
    }

    @Test
    void readMessageMarksTheMessageWhenOneOfTheUsersGroupsTakesPart() {
        authenticate(USER);
        when(messagingService.userGroupIds(USER)).thenReturn(List.of("g0", "g1"));
        participantGroup = "g1";

        ThreadDTO result = controller.readMessage("t1", "0", true, "someone-else@example.org").block();

        assertEquals("t1", result.getId());
        assertTrue(sent("PATCH /threads/t1/messages/0?read=true&userId=" + USER));
    }

    @Test
    void readMessageIsDeniedWhenTheUserDoesNotTakePart() {
        authenticate(USER);
        when(messagingService.userGroupIds(anyString())).thenReturn(List.of("g0"));

        assertThrows(AccessDeniedException.class, () -> controller.readMessage("t1", "0", true, USER).block());
        assertTrue(sent("GET /threads/t1?email=" + USER + "&groupId=g0"));
        assertTrue(!sent("PATCH"));
    }

    @Test
    void readMessageForAUserWithoutGroupsMatchesOnlyTheEmail() {
        authenticate(USER);
        when(messagingService.userGroupIds(anyString())).thenReturn(List.of());

        assertThrows(AccessDeniedException.class, () -> controller.readMessage("t1", "0", true, USER).block());
        assertTrue(sent("GET /threads/t1?email=" + USER + "&groupId="));
        assertTrue(!sent("PATCH"));
    }

    @Test
    void addIsDeniedWhenTheSenderIsAnotherUser() {
        authenticate(USER);

        assertThrows(AccessDeniedException.class, () -> controller.add(thread("other@example.org", "g1")));
        assertTrue(!sent("POST"));
    }

    @Test
    void addIsDeniedOutsideTheSendersGroup() {
        authenticate(USER);

        assertThrows(AccessDeniedException.class, () -> controller.add(thread(USER, "g2")));
    }

    @Test
    void addSendsTheThreadFromTheUsersOwnEmailAndGroup() {
        authenticate(USER);

        assertEquals("t1", controller.add(thread(USER, "g1")).block().getId());
        assertTrue(sent("POST /threads"));
    }

    @Test
    void updateAndDeleteRequireTheAdminAuthority() {
        authenticate(USER);
        assertThrows(AccessDeniedException.class, () -> controller.update("t1", new TopicThread()));
        assertThrows(AccessDeniedException.class, () -> controller.delete("t1"));

        authenticate(USER, "ADMIN");
        controller.update("t1", new TopicThread()).block();
        controller.delete("t1").block();
        assertTrue(sent("PUT /threads/t1"));
        assertTrue(sent("DELETE /threads/t1"));
    }
}
