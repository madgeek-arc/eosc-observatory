package eu.eosc.observatory.controller;

import eu.eosc.observatory.configuration.ApplicationProperties;
import eu.eosc.observatory.domain.Coordinator;
import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.domain.UserInfo;
import eu.eosc.observatory.service.CrudItemService;
import eu.eosc.observatory.service.UserService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import gr.athenarc.catalogue.annotations.Browse;
import gr.athenarc.catalogue.controller.GenericItemController;
import gr.athenarc.catalogue.exception.ResourceNotFoundException;

import gr.athenarc.catalogue.utils.PagingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final CrudItemService<Stakeholder> stakeholderService;
    private final CrudItemService<Coordinator> coordinatorService;

    private final ApplicationProperties applicationProperties;

    @Autowired
    public UserController(UserService userService,
                          CrudItemService<Stakeholder> stakeholderService,
                          CrudItemService<Coordinator> coordinatorService,
                          ApplicationProperties applicationProperties) {
        this.userService = userService;
        this.stakeholderService = stakeholderService;
        this.coordinatorService = coordinatorService;
        this.applicationProperties = applicationProperties;
    }

    @RequestMapping(value = "refreshLogin", method = RequestMethod.HEAD)
    @PreAuthorize("not isAnonymous()")
    public void refreshLogin(HttpServletRequest request, HttpServletResponse response, @Parameter(hidden = true) Authentication authentication) throws IOException, ServletException {
        if (authentication != null && authentication.getPrincipal() != null) {
            Cookie cookie = new Cookie("AccessToken", ((OidcUser) authentication.getPrincipal()).getIdToken().getTokenValue());
            cookie.setMaxAge(3600);
            cookie.setPath("/");
            response.addCookie(cookie);
        }
    }

    @Parameter(hidden = true)
    @GetMapping("user/oidc-principal")
    public OidcUser getOidcUserPrincipal(@AuthenticationPrincipal OidcUser principal) {
        return principal;
    }

    @GetMapping("user/info")
    @PreAuthorize("not isAnonymous()")
    public ResponseEntity<UserInfo> userInfo(@Parameter(hidden = true) Authentication authentication) {
        UserInfo userInfo = getUserInfo(authentication);
        return new ResponseEntity<>(userInfo, HttpStatus.OK);
    }

    @GetMapping("users/{id}/info")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserInfo> userInfo(@PathVariable("id") String userId) {
        return new ResponseEntity<>(getUserInfo(userId), HttpStatus.OK);
    }

    @GetMapping("users/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<User> getUser(@PathVariable("id") String userId) {
        User user = userService.get(userId);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PutMapping("users/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<User> updateUser(@PathVariable("id") String userId, @RequestBody User user) throws eu.openminted.registry.core.exception.ResourceNotFoundException {
        user = userService.update(userId, user);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @Browse
    @GetMapping("users")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Browsing<User>> getUsers(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter filter = PagingUtils.createFacetFilter(allRequestParams);
        Browsing<User> users = userService.getAll(filter);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @PatchMapping("user/policies/{id}")
    public ResponseEntity<User> acceptPolicy(@PathVariable(value = "id") String id, @Parameter(hidden = true) Authentication authentication) {
        User user = userService.acceptPrivacyPolicy(id, authentication);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @DeleteMapping("users/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<User> delete(@PathVariable("id") String userId) throws eu.openminted.registry.core.exception.ResourceNotFoundException {
        User user = userService.delete(userId);
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

    private UserInfo getUserInfo(Authentication authentication) {
        User user = User.of(authentication);
        return createUserInfo(user);
    }

    private UserInfo getUserInfo(String userId) {
        User user;
        try {
            user = userService.get(userId);
        } catch (ResourceNotFoundException e) {
            user = new User();
            user.setEmail(userId);
        }

        return createUserInfo(user);
    }

    private UserInfo createUserInfo(User user) {
        UserInfo info = new UserInfo();
        info.setUser(user);
        info.setAdmin(applicationProperties.getAdmins().contains(user.getEmail()));
        info.setStakeholders(new HashSet<>());
        info.setCoordinators(new HashSet<>());

        info.getStakeholders().addAll(stakeholderService.getWithFilter("managers", user.getId()));
        info.getStakeholders().addAll(stakeholderService.getWithFilter("contributors", user.getId()));
        info.getCoordinators().addAll(getCoordinatorsWithFilter("members", user.getId()));
        return info;
    }
}
