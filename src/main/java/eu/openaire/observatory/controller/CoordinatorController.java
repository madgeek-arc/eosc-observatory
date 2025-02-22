/**
 * Copyright 2021-2025 OpenAIRE AMKE
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

import eu.openaire.observatory.domain.Coordinator;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.dto.GroupMembers;
import eu.openaire.observatory.service.CoordinatorService;
import eu.openaire.observatory.service.UserService;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("coordinators")
public class CoordinatorController {

    private static final Logger logger = LoggerFactory.getLogger(CoordinatorController.class);

    private final CoordinatorService coordinatorService;
    private final UserService userService;

    public CoordinatorController(CoordinatorService coordinatorService,
                                 UserService userService) {
        this.coordinatorService = coordinatorService;
        this.userService = userService;
    }

    /*---------------------------*/
    /*        CRUD methods       */
    /*---------------------------*/

    @GetMapping("{id}")
    @PreAuthorize("isCoordinator(#id)")
    public ResponseEntity<Coordinator> get(@PathVariable("id") String id) {
        return new ResponseEntity<>(coordinatorService.get(id), HttpStatus.OK);
    }

    @PostMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Coordinator> create(@RequestBody Coordinator coordinator) {
        return new ResponseEntity<>(coordinatorService.add(coordinator), HttpStatus.CREATED);
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN')")// or isCoordinatorManager(#coordinatorId)")
    public ResponseEntity<Coordinator> update(@PathVariable("id") String id, @RequestBody Coordinator coordinator) throws ResourceNotFoundException {
        return new ResponseEntity<>(coordinatorService.update(id, coordinator), HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Coordinator> delete(@PathVariable("id") String id) throws ResourceNotFoundException {
        return new ResponseEntity<>(coordinatorService.delete(id), HttpStatus.OK);
    }

    @GetMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Browsing<Coordinator>> getCoordinators(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter filter = FacetFilter.from(allRequestParams);
        Browsing<Coordinator> coordinators = coordinatorService.getAll(filter);
        return new ResponseEntity<>(coordinators, HttpStatus.OK);
    }

    /*---------------------------*/
    /*       Member methods      */
    /*---------------------------*/

    @GetMapping("{id}/users")
    @PreAuthorize("hasAuthority('ADMIN') or isCoordinatorMember(#coordinatorId)")
    public ResponseEntity<GroupMembers<User>> getUsers(@PathVariable("id") String coordinatorId) {
        return new ResponseEntity<>(coordinatorService.getGroupMembers(coordinatorId).map(userService::getUser), HttpStatus.OK);
    }

    @GetMapping("{id}/members")
    @PreAuthorize("hasAuthority('ADMIN')")// or isCoordinatorMember(#coordinatorId)")
    public ResponseEntity<GroupMembers<User>> getMembers(@PathVariable("id") String coordinatorId) {
        return new ResponseEntity<>(coordinatorService.getGroupMembers(coordinatorId).map(userService::getUser), HttpStatus.OK);
    }

    @PatchMapping("{id}/members")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Set<?>> updateMembers(@PathVariable("id") String coordinatorId, @RequestBody Set<String> emails) {
        return new ResponseEntity<>(coordinatorService.updateMembers(coordinatorId, emails), HttpStatus.OK);
    }


    @PostMapping("{id}/members")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Set<String>> addMember(@PathVariable("id") String coordinatorId, @RequestBody String email) {
        return new ResponseEntity<>(coordinatorService.addMember(coordinatorId, email), HttpStatus.OK);
    }

    @DeleteMapping("{id}/members/{memberId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Set<String>> removeMember(@PathVariable("id") String coordinatorId, @PathVariable("memberId") String memberId) {
        return new ResponseEntity<>(coordinatorService.removeMember(coordinatorId, memberId), HttpStatus.OK);
    }

    /*---------------------------*/
    /*       Other methods       */
    /*---------------------------*/


}

