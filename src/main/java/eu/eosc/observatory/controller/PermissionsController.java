package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.service.PermissionsService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
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

import java.util.Set;

@RestController
@RequestMapping("permissions")
public class PermissionsController {

    private static final Logger logger = LogManager.getLogger(PermissionsController.class);

    private final PermissionsService permissionsService;

    @Autowired
    public PermissionsController(PermissionsService permissionsService) {
        this.permissionsService = permissionsService;
    }

    @GetMapping
    @PreAuthorize("hasPermission(#resourceId, 'read')")
    public ResponseEntity<Set<String>> getPermissions(@RequestParam("resourceId") String resourceId, @ApiIgnore Authentication authentication) {
        return new ResponseEntity<>(permissionsService.getPermissions(User.getId(authentication), resourceId), HttpStatus.OK);
    }

    @GetMapping("admin")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Set<String>> getUserPermissions(@RequestParam("userId") String userId, @RequestParam("resourceId") String resourceId) {
        return new ResponseEntity<>(permissionsService.getPermissions(userId, resourceId), HttpStatus.OK);
    }
}
