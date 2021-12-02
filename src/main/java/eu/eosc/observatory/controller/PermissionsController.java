package eu.eosc.observatory.controller;

import eu.eosc.observatory.service.PermissionsService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<Set<String>> getPermissions(@RequestParam("userId") String userId, @RequestParam("resourceId") String resourceId) {
        return new ResponseEntity<>(permissionsService.getPermissions(userId, resourceId), HttpStatus.OK);
    }
}
