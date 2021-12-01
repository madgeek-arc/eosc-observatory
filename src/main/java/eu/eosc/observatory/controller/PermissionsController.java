package eu.eosc.observatory.controller;

import eu.eosc.observatory.service.PermissionsService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<String>> getPermissions(@RequestParam("userId") String userId, @RequestParam("resourceId") String resourceId) {
        return new ResponseEntity<>(permissionsService.getPermissions(userId, resourceId), HttpStatus.OK);
    }
}
