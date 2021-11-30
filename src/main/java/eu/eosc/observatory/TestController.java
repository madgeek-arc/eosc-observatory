package eu.eosc.observatory;

import gr.athenarc.authorization.service.Authorization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("test")
public class TestController {

    private final Authorization authorizationService;

    @Autowired
    public TestController(Authorization authorization) {
        this.authorizationService = authorization;
    }

    @GetMapping
    @PreAuthorize("@authorizationService.canDo('user1', 'read', 'resource1') == true ")
    public boolean canDo() {
        return true;
    }

    @GetMapping("/oidc-principal")
    public OidcUser getOidcUserPrincipal(
            @AuthenticationPrincipal OidcUser principal) {
        return principal;
    }
}
