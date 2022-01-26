package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.*;
import eu.eosc.observatory.dto.UserPrivacyPolicyInfo;
import eu.eosc.observatory.service.CrudItemService;
import eu.eosc.observatory.service.PrivacyPolicyService;
import eu.eosc.observatory.service.UserService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("user")
public class UserController {

    private static final Logger logger = LogManager.getLogger(UserController.class);

    private final UserService userService;
    private final CrudItemService<Stakeholder> stakeholderService;
    private final CrudItemService<Coordinator> coordinatorService;

    @Autowired
    public UserController(UserService userService,
                          CrudItemService<Stakeholder> stakeholderService,
                          CrudItemService<Coordinator> coordinatorService) {
        this.userService = userService;
        this.stakeholderService = stakeholderService;
        this.coordinatorService = coordinatorService;
    }

    @ApiIgnore
    @GetMapping("/oidc-principal")
    public OidcUser getOidcUserPrincipal(@AuthenticationPrincipal OidcUser principal) {
        return principal;
    }

    @GetMapping("info")
    public ResponseEntity<UserInfo> userInfo(@ApiIgnore Authentication authentication) {
        // TODO: move body to a method
        User user = userService.get(User.getId(authentication));
        UserInfo info = new UserInfo();
        info.setUser(user);
        info.setStakeholders(new HashSet<>());
        info.setCoordinators(new HashSet<>());

        info.getStakeholders().addAll(stakeholderService.getWithFilter("managers", user.getId()));
        info.getStakeholders().addAll(stakeholderService.getWithFilter("contributors", user.getId()));
        info.getCoordinators().addAll(getCoordinatorsWithFilter("members", user.getId()));

        return new ResponseEntity<>(info, HttpStatus.OK);
    }

    @PatchMapping("/consent")
    public ResponseEntity<Void> setConsent(@RequestParam(value = "consent", defaultValue = "false") boolean consent, @ApiIgnore Authentication authentication) throws ResourceNotFoundException {
        userService.updateUserConsent(User.getId(authentication), consent);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("policies/{id}")
    public ResponseEntity<User> acceptPolicy(@PathVariable(value = "id") String id, @ApiIgnore Authentication authentication) {
        User user = userService.acceptPrivacyPolicy(id, authentication);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    private Set<Coordinator> getCoordinatorsWithFilter(String key, String value) {
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);
        filter.addFilter(key, value);
        Browsing<Coordinator> results = coordinatorService.getAll(filter);
        return results.getResults()
                .stream()
                .collect(Collectors.toSet());
    }
}
