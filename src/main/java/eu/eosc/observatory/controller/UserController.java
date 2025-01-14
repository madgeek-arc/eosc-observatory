package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.Profile;
import eu.eosc.observatory.domain.Settings;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.domain.UserInfo;
import eu.eosc.observatory.dto.ProfileDTO;
import eu.eosc.observatory.service.UserService;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
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
    public ResponseEntity<UserInfo> userInfo(@Parameter(hidden = true) Authentication authentication) {
        UserInfo userInfo = userService.getUserInfo(authentication);
        return new ResponseEntity<>(userInfo, HttpStatus.OK);
    }

    @PatchMapping("user/policies/{id}")
    public ResponseEntity<User> acceptPolicy(@PathVariable(value = "id") String id, @Parameter(hidden = true) Authentication authentication) {
        User user = userService.acceptPrivacyPolicy(id, authentication);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PutMapping("users/{id}/profile")
    @PreAuthorize("hasAuthority('ADMIN') or (isAuthenticated() and authentication.principal.getAttribute('email') == #userId)")
    public ResponseEntity<User> updateUserProfile(@PathVariable("id") String userId, @RequestBody ProfileDTO profileDTO) throws gr.uoa.di.madgik.registry.exception.ResourceNotFoundException {
        User user = userService.getUser(userId);
        user.setProfile(Profile.of(profileDTO));
        user = userService.update(userId, user);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PostMapping("users/{id}/profile/picture")
    @PreAuthorize("hasAuthority('ADMIN') or (isAuthenticated() and authentication.principal.getAttribute('email') == #userId)")
    public ResponseEntity<User> updateUserPicture(@PathVariable("id") String userId, @RequestPart(name = "picture") MultipartFile picture) throws gr.uoa.di.madgik.registry.exception.ResourceNotFoundException, IOException {
        User user = userService.getUser(userId);
        if (user.getProfile() == null) {
            user.setProfile(new Profile());
        }
        user.getProfile().setPicture(picture.getBytes());
        user = userService.update(userId, user);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PutMapping("users/{id}/settings")
    @PreAuthorize("hasAuthority('ADMIN') or (isAuthenticated() and authentication.principal.getAttribute('email') == #userId)")
    public ResponseEntity<User> updateUserSettings(@PathVariable("id") String userId, @RequestBody Settings settings) throws gr.uoa.di.madgik.registry.exception.ResourceNotFoundException {
        User user = userService.getUser(userId);
        user.setSettings(settings);
        user = userService.update(userId, user);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }


    // ADMIN METHODS

    @GetMapping("users")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Browsing<User>> getUsers(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter filter = FacetFilter.from(allRequestParams);
        Browsing<User> users = userService.getAll(filter);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("users/{id}/info")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserInfo> userInfo(@PathVariable("id") String userId) {
        return new ResponseEntity<>(userService.getUserInfo(userId), HttpStatus.OK);
    }

    @GetMapping("users/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<User> getUser(@PathVariable("id") String userId) {
        User user = userService.get(userId);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PutMapping("users/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<User> updateUser(@PathVariable("id") String userId, @RequestBody User user) throws gr.uoa.di.madgik.registry.exception.ResourceNotFoundException {
        user = userService.update(userId, user);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @DeleteMapping("users/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<User> delete(@PathVariable("id") String userId) throws gr.uoa.di.madgik.registry.exception.ResourceNotFoundException {
        User user = userService.delete(userId);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }
}
