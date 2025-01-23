package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.service.InvitationService;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("invitation")
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
