/*
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

import eu.openaire.observatory.domain.Administrator;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.dto.GroupMembers;
import eu.openaire.observatory.service.AdministratorService;
import eu.openaire.observatory.service.UserService;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping(path = "administrators", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdministratorController {

    private static final Logger logger = LoggerFactory.getLogger(AdministratorController.class);

    private final AdministratorService administratorService;
    private final UserService userService;

    public AdministratorController(AdministratorService administratorService,
                                   UserService userService) {
        this.administratorService = administratorService;
        this.userService = userService;
    }

    /*---------------------------*/
    /*        CRUD methods       */
    /*---------------------------*/

    @GetMapping("{id}")
    @PreAuthorize("isAdministrator(#id)")
    public ResponseEntity<Administrator> get(@PathVariable("id") String id) {
        return new ResponseEntity<>(administratorService.get(id), HttpStatus.OK);
    }

    @PostMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Administrator> create(@RequestBody Administrator administrator) {
        return new ResponseEntity<>(administratorService.add(administrator), HttpStatus.CREATED);
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN')")// or isAdministratorManager(#administratorId)")
    public ResponseEntity<Administrator> update(@PathVariable("id") String id, @RequestBody Administrator administrator) throws ResourceNotFoundException {
        return new ResponseEntity<>(administratorService.update(id, administrator), HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Administrator> delete(@PathVariable("id") String id) throws ResourceNotFoundException {
        return new ResponseEntity<>(administratorService.delete(id), HttpStatus.OK);
    }

    @GetMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Browsing<Administrator>> getAdministrators(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter filter = FacetFilter.from(allRequestParams);
        Browsing<Administrator> administrators = administratorService.getAll(filter);
        return new ResponseEntity<>(administrators, HttpStatus.OK);
    }

    /*---------------------------*/
    /*       Member methods      */
    /*---------------------------*/


    @GetMapping("{id}/users")
    @PreAuthorize("hasAuthority('ADMIN') or isAdministrator(#administratorId)")
    public ResponseEntity<GroupMembers<User>> getUsers(@PathVariable("id") String administratorId) {
        return new ResponseEntity<>(administratorService.getGroupMembers(administratorId).map(userService::getUser), HttpStatus.OK);
    }

    @GetMapping("{id}/members")
    @PreAuthorize("hasAuthority('ADMIN')")// or isAdministratorMember(#administratorId)")
    public ResponseEntity<Set<String>> getMembers(@PathVariable("id") String administratorId) {
        return new ResponseEntity<>(administratorService.getMembers(administratorId), HttpStatus.OK);
    }

    @PatchMapping("{id}/members")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Set<?>> updateMembers(@PathVariable("id") String administratorId, @RequestBody Set<String> emails) {
        return new ResponseEntity<>(administratorService.updateMembers(administratorId, emails), HttpStatus.OK);
    }


    @PostMapping("{id}/members")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Set<String>> addMember(@PathVariable("id") String administratorId, @RequestBody String email) {
        return new ResponseEntity<>(administratorService.addMember(administratorId, email), HttpStatus.OK);
    }

    @DeleteMapping("{id}/members/{memberId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Set<String>> removeMember(@PathVariable("id") String administratorId, @PathVariable("memberId") String memberId) {
        return new ResponseEntity<>(administratorService.removeMember(administratorId, memberId), HttpStatus.OK);
    }

    /*---------------------------*/
    /*       Other methods       */
    /*---------------------------*/


}

