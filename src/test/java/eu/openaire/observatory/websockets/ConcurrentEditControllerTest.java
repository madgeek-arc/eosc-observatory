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

package eu.openaire.observatory.websockets;

import eu.openaire.observatory.domain.Revision;
import eu.openaire.observatory.permissions.Permissions;
import eu.openaire.observatory.service.SecurityService;
import eu.openaire.observatory.service.SurveyService;
import eu.openaire.observatory.utils.OidcTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConcurrentEditControllerTest {

    private final SurveyService surveyService = mock(SurveyService.class);
    private final SecurityService securityService = mock(SecurityService.class);
    private final ConcurrentEditController controller =
            new ConcurrentEditController(surveyService, securityService);

    @Test
    void permittedEditCallsThroughAndReturnsRevision() {
        Authentication auth = OidcTestUtils.oidcAuthentication("colleague@example.org");
        Revision revision = new Revision();
        revision.setField("answer");
        when(securityService.hasPermission(auth, Permissions.WRITE.getKey(), "sa-1")).thenReturn(true);

        Revision result = controller.editField("session-1", "survey_answer", "sa-1", revision, auth);

        assertSame(revision, result);
        assertEquals("session-1", result.getSessionId());
        verify(surveyService).edit("sa-1", revision, auth);
    }

    @Test
    void deniedEditThrowsAndNeverCallsSurveyService() {
        Authentication auth = OidcTestUtils.oidcAuthentication("colleague@example.org");
        Revision revision = new Revision();
        when(securityService.hasPermission(auth, Permissions.WRITE.getKey(), "sa-1")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> controller.editField("session-1", "survey_answer", "sa-1", revision, auth));

        assertNull(revision.getSessionId());
        verifyNoInteractions(surveyService);
    }

    @Test
    void exceptionHandlerReturnsExceptionMessage() {
        AccessDeniedException ex = new AccessDeniedException("You are not allowed to edit this survey answer.");

        String result = controller.handleException(ex);

        assertEquals("You are not allowed to edit this survey answer.", result);
    }
}
