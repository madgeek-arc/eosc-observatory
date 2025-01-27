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
package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.User;
import org.springframework.security.core.Authentication;

import java.util.Date;

public interface InvitationService {

    String createInvitation(User inviter, String inviteeEmail, String role, String stakeholderId);

    String createInvitation(User inviter, String inviteeEmail, String role, String stakeholderId, Date expiration);

    boolean acceptInvitation(String invitation, Authentication authentication);

}
