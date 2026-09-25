package eu.openaire.observatory.controller;

import eu.openaire.observatory.dto.DashboardOverrideSummary;
import eu.openaire.observatory.service.DashboardDefaultsService;
import eu.openaire.observatory.service.DashboardOverrideService;
import eu.openaire.observatory.widget.Widget;
import eu.openaire.observatory.widget.DashboardDefaults;
import eu.openaire.observatory.widget.DashboardOverrides;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class DashboardController {

    private static final String COUNTRY_PAGES_CODE = "country-pages";

    private final DashboardDefaultsService defaultsService;
    private final DashboardOverrideService overrideService;

    public DashboardController(DashboardDefaultsService defaultsService,
                             DashboardOverrideService overrideService) {
        this.defaultsService = defaultsService;
        this.overrideService = overrideService;
    }

    /*---------------------------*/
    /*     Default Indicators    */
    /*---------------------------*/

    @GetMapping("indicators/defaults/{type}")
    public ResponseEntity<DashboardDefaults> getDefaults(@PathVariable("type") String type) {
        return defaultsService.getByCodeAndType(COUNTRY_PAGES_CODE, type)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("indicators/defaults")
    @PreAuthorize("hasAuthority('ADMIN') or isCoordinatorOfType(#defaults.getType()) or isAdministratorOfType(#defaults.getType())")
    public ResponseEntity<DashboardDefaults> createDefaults(@RequestBody DashboardDefaults defaults) {
        defaults.setCode(COUNTRY_PAGES_CODE);
        return new ResponseEntity<>(defaultsService.add(defaults), HttpStatus.CREATED);
    }

    @PutMapping("indicators/defaults/{type}")
    @PreAuthorize("hasAuthority('ADMIN') or isCoordinatorOfType(#type) or isAdministratorOfType(#type)")
    public ResponseEntity<DashboardDefaults> updateDefaults(@PathVariable("type") String type,
                                                          @RequestBody DashboardDefaults defaults) throws ResourceNotFoundException {
        defaults.setCode(COUNTRY_PAGES_CODE);
        return ResponseEntity.ok(defaultsService.updateByCodeAndType(COUNTRY_PAGES_CODE, type, defaults));
    }

    @DeleteMapping("indicators/defaults/{type}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<DashboardDefaults> deleteDefaults(@PathVariable("type") String type) throws ResourceNotFoundException {
        return ResponseEntity.ok(defaultsService.deleteByCodeAndType(COUNTRY_PAGES_CODE, type));
    }

    /*---------------------------*/
    /*   Stakeholder Indicators  */
    /*---------------------------*/

    @GetMapping("stakeholders/types/{type}/indicators/overrides")
    public ResponseEntity<List<DashboardOverrideSummary>> getStakeholdersOverrideStatus(@PathVariable("type") String type) {
        return ResponseEntity.ok(overrideService.getStakeholdersWithOverrideStatus(COUNTRY_PAGES_CODE, type));
    }

    @GetMapping("stakeholders/{stakeholderId}/indicators")
    public ResponseEntity<List<Widget>> getEffectiveIndicators(@PathVariable("stakeholderId") String stakeholderId) {
        return ResponseEntity.ok(overrideService.getEffectiveWidgets(COUNTRY_PAGES_CODE, stakeholderId));
    }

    @GetMapping("stakeholders/{stakeholderId}/indicators/overrides")
    public ResponseEntity<DashboardOverrides> getOverrides(@PathVariable("stakeholderId") String stakeholderId) {
        return overrideService.getByCodeAndStakeholderId(COUNTRY_PAGES_CODE, stakeholderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("stakeholders/{stakeholderId}/indicators/overrides")
    @PreAuthorize("hasAuthority('ADMIN') or isAdministratorOfStakeholder(#stakeholderId) or isCoordinatorOfStakeholder(#stakeholderId)")
    public ResponseEntity<DashboardOverrides> upsertOverrides(@PathVariable("stakeholderId") String stakeholderId,
                                                           @RequestBody DashboardOverrides overrides) throws ResourceNotFoundException {
        overrides.setStakeholderId(stakeholderId);
        overrides.setCode(COUNTRY_PAGES_CODE);
        return ResponseEntity.ok(overrideService.upsert(overrides));
    }

    @DeleteMapping("stakeholders/{stakeholderId}/indicators/overrides")
    @PreAuthorize("hasAuthority('ADMIN') or isAdministratorOfStakeholder(#stakeholderId) or isCoordinatorOfStakeholder(#stakeholderId)")
    public ResponseEntity<DashboardOverrides> deleteOverrides(@PathVariable("stakeholderId") String stakeholderId) throws ResourceNotFoundException {
        return ResponseEntity.ok(overrideService.deleteByCodeAndStakeholderId(COUNTRY_PAGES_CODE, stakeholderId));
    }
}
