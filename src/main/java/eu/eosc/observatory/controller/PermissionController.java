package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.dto.ResourcePermissions;
import eu.eosc.observatory.permissions.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("permissions")
public class PermissionController {

    private static final Logger logger = LoggerFactory.getLogger(PermissionController.class);

    private final PermissionService permissionService;

    @Autowired
    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public ResponseEntity<List<ResourcePermissions>> getResourcePermissions(@RequestParam("resourceIds") List<String> resourceIds, @ApiIgnore Authentication authentication) {
        return new ResponseEntity<>(permissionService.getResourcePermissions(User.getId(authentication), resourceIds), HttpStatus.OK);
    }

    @GetMapping("admin")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Set<String>> getUserPermissions(@RequestParam("userId") String userId, @RequestParam("resourceId") String resourceId) {
        return new ResponseEntity<>(permissionService.getPermissions(userId, resourceId), HttpStatus.OK);
    }
}
