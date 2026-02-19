/*
 * Copyright 2021-2026 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.openaire.observatory.controller;

import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.dto.ResourcePermissions;
import eu.openaire.observatory.permissions.PermissionService;
import gr.uoa.di.madgik.authorization.domain.Permission;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(path = "permissions", produces = MediaType.APPLICATION_JSON_VALUE)
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
