package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.dto.ResourcePermissions;
import eu.eosc.observatory.permissions.PermissionService;
import gr.athenarc.authorization.domain.Permission;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("permissions")
public class PermissionController {

    private static final Logger logger = LoggerFactory.getLogger(PermissionController.class);

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public ResponseEntity<Collection<ResourcePermissions>> getResourcePermissions(@RequestParam("resourceIds") List<String> resourceIds, @Parameter(hidden = true) Authentication authentication) {
        return new ResponseEntity<>(permissionService.getResourcePermissions(User.getId(authentication), resourceIds), HttpStatus.OK);
    }

    @GetMapping("admin")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Set<String>> getUserPermittedActions(@RequestParam("userId") String userId, @RequestParam("resourceId") String resourceId) {
        return new ResponseEntity<>(permissionService.getPermissions(userId, resourceId), HttpStatus.OK);
    }

    @GetMapping("users/{userId}/actions/{action}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Set<Permission>> getUserPermissionsByAction(@PathVariable("userId") String userId, @PathVariable("action") String action) {
        return new ResponseEntity<>(permissionService.getUserPermissionsByAction(userId, action), HttpStatus.OK);
    }
}
