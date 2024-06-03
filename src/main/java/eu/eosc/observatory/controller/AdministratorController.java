package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.Administrator;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.dto.GroupMembers;
import eu.eosc.observatory.service.AdministratorService;
import eu.eosc.observatory.service.UserService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.annotations.Browse;
import gr.athenarc.catalogue.utils.PagingUtils;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("administrators")
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

    @Browse
    @GetMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Browsing<Administrator>> getAdministrators(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter filter = PagingUtils.createFacetFilter(allRequestParams);
        Browsing<Administrator> administrators = administratorService.getAll(filter);
        return new ResponseEntity<>(administrators, HttpStatus.OK);
    }

    /*---------------------------*/
    /*       Member methods      */
    /*---------------------------*/


    @GetMapping("{id}/users")
    @PreAuthorize("hasAuthority('ADMIN') or isAdministratorMember(#administratorId)")
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

