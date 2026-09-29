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

package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.Invitation;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.dto.InvitationResultDTO;
import org.springframework.security.core.Authentication;

import java.util.Date;

public interface InvitationService {

    /**
     * Creates an invitation token and emails the accept link to the invitee. The invitation is
     * self-contained in the token; nothing is stored.
     */
    InvitationResultDTO createInvitation(User inviter, String inviteeEmail, String role, Invitation.Group group, String groupId);

    InvitationResultDTO createInvitation(User inviter, String inviteeEmail, String role, Invitation.Group group, String groupId, Date expiration);

    /**
     * Accepts the invitation for the authenticated user.
     *
     * @return false if the token is invalid, tampered, expired, addressed to another user, or the
     * inviter no longer has the right to invite.
     */
    boolean acceptInvitation(String token, Authentication authentication);

}
