/*
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
package eu.openaire.observatory.controller;

import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.service.InvitationService;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "invitation", produces = MediaType.APPLICATION_JSON_VALUE)
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> invitationToken(@RequestParam("inviteeEmail") String invitee,
                                                  @RequestParam("inviteeRole") String role,
                                                  @RequestParam("stakeholder") String stakeholderId,
                                                  @Parameter(hidden = true) Authentication authentication) {
        return new ResponseEntity<>(invitationService.createInvitation(User.of(authentication), invitee, role, stakeholderId), HttpStatus.OK);
    }

    @GetMapping("accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> acceptInvitation(@RequestParam("invitationToken") String token,
                                                 @Parameter(hidden = true) Authentication authentication) {
        if (!invitationService.acceptInvitation(token, authentication)) {
            throw new ResourceException("Invalid invitation.", HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
