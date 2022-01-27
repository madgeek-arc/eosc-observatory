package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.service.InvitationService;
import gr.athenarc.catalogue.exception.ResourceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("invitation")
public class InvitationController {

    private final InvitationService invitationService;

    @Autowired
    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")

    public ResponseEntity<String> invitationToken(@RequestParam("inviteeEmail") String invitee,
                                                  @RequestParam("inviteeRole") String role,
                                                  @RequestParam("stakeholder") String stakeholderId,
                                                  @ApiIgnore Authentication authentication) {
        return new ResponseEntity<>(invitationService.createInvitation(User.of(authentication), invitee, role, stakeholderId), HttpStatus.OK);
    }

    @GetMapping("accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> acceptInvitation(@RequestParam("invitationToken") String token) {
        if (!invitationService.acceptInvitation(token)) {
            throw new ResourceException("Invalid invitation.", HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
