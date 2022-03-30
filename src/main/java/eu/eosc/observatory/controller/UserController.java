package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.*;
import eu.eosc.observatory.service.CrudItemService;
import eu.eosc.observatory.service.UserService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import gr.athenarc.catalogue.controller.GenericItemController;
import gr.athenarc.catalogue.exception.ResourceNotFoundException;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.HashSet;
import java.util.Map;
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
        UserInfo userInfo = getUserInfo(User.of(authentication).getId());
        return new ResponseEntity<>(userInfo, HttpStatus.OK);
    }

    @GetMapping("info/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserInfo> userInfo(@PathVariable("id") String userId) {
        return new ResponseEntity<>(getUserInfo(userId), HttpStatus.OK);
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<User> getUser(@PathVariable("id") String userId) {
        User user = userService.get(userId);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<User> updateUser(@PathVariable("id") String userId, @RequestBody User user) throws eu.openminted.registry.core.exception.ResourceNotFoundException {
        user = userService.update(userId, user);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataTypeClass = String.class, paramType = "query")
    })
    @GetMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Browsing<User>> getUsers(@ApiIgnore @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter filter = GenericItemController.createFacetFilter(allRequestParams);
        Browsing<User> users = userService.getAll(filter);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @PatchMapping("policies/{id}")
    public ResponseEntity<User> acceptPolicy(@PathVariable(value = "id") String id, @ApiIgnore Authentication authentication) {
        User user = userService.acceptPrivacyPolicy(id, authentication);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @DeleteMapping("{id}")
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

    private UserInfo getUserInfo(String userId) {
        User user;
        try {
            user = userService.get(userId);
        } catch (ResourceNotFoundException e) {
            user = new User();
            user.setEmail(userId);
        }
        UserInfo info = new UserInfo();
        info.setUser(user);
        info.setStakeholders(new HashSet<>());
        info.setCoordinators(new HashSet<>());

        info.getStakeholders().addAll(stakeholderService.getWithFilter("managers", user.getId()));
        info.getStakeholders().addAll(stakeholderService.getWithFilter("contributors", user.getId()));
        info.getCoordinators().addAll(getCoordinatorsWithFilter("members", user.getId()));
        return info;
    }
}
