package eu.openaire.observatory.controller;

import eu.openaire.observatory.dto.DashboardOverrideSummary;
import eu.openaire.observatory.service.DashboardDefaultsService;
import eu.openaire.observatory.service.DashboardOverrideService;
import eu.openaire.observatory.widget.DashboardCode;
import eu.openaire.observatory.widget.Widget;
import eu.openaire.observatory.widget.DashboardDefaults;
import eu.openaire.observatory.widget.DashboardOverrides;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping(value = "dashboards", produces = MediaType.APPLICATION_JSON_VALUE)
public class DashboardController {

    private final DashboardDefaultsService defaultsService;
    private final DashboardOverrideService overrideService;

    public DashboardController(DashboardDefaultsService defaultsService,
                             DashboardOverrideService overrideService) {
        this.defaultsService = defaultsService;
        this.overrideService = overrideService;
    }

    private static DashboardCode resolve(String codeValue) {
        try {
            return DashboardCode.fromWireValue(codeValue);
        } catch (IllegalArgumentException e) {
            throw new ResourceException("Unknown dashboard code: " + codeValue, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping
    public ResponseEntity<List<String>> getDashboardCodes() {
        return ResponseEntity.ok(Arrays.stream(DashboardCode.values()).map(DashboardCode::wireValue).toList());
    }

    /*---------------------------*/
    /*     Default Widgets       */
    /*---------------------------*/

    @GetMapping("{code}/types")
    public ResponseEntity<List<DashboardDefaults>> getDefaultsForDashboard(@PathVariable("code") String codeValue) {
        DashboardCode code = resolve(codeValue);
        return ResponseEntity.ok(defaultsService.getByCode(code));
    }

    @GetMapping("{code}/types/{type}")
    public ResponseEntity<DashboardDefaults> getDefaults(@PathVariable("code") String codeValue,
                                                       @PathVariable("type") String type) {
        DashboardCode code = resolve(codeValue);
        return defaultsService.getByCodeAndType(code, type)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("{code}/types")
    @PreAuthorize("hasAuthority('ADMIN') or isCoordinatorOfType(#defaults.getType()) or isAdministratorOfType(#defaults.getType())")
    public ResponseEntity<DashboardDefaults> createDefaults(@PathVariable("code") String codeValue,
                                                          @RequestBody DashboardDefaults defaults) {
        DashboardCode code = resolve(codeValue);
        defaults.setCode(code);
        return new ResponseEntity<>(defaultsService.add(defaults), HttpStatus.CREATED);
    }

    @PutMapping("{code}/types/{type}")
    @PreAuthorize("hasAuthority('ADMIN') or isCoordinatorOfType(#type) or isAdministratorOfType(#type)")
    public ResponseEntity<DashboardDefaults> updateDefaults(@PathVariable("code") String codeValue,
                                                          @PathVariable("type") String type,
                                                          @RequestBody DashboardDefaults defaults) throws ResourceNotFoundException {
        DashboardCode code = resolve(codeValue);
        defaults.setCode(code);
        defaults.setType(type);
        return ResponseEntity.ok(defaultsService.upsertByCodeAndType(code, type, defaults));
    }

    @DeleteMapping("{code}/types/{type}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<DashboardDefaults> deleteDefaults(@PathVariable("code") String codeValue,
                                                          @PathVariable("type") String type) throws ResourceNotFoundException {
        DashboardCode code = resolve(codeValue);
        return ResponseEntity.ok(defaultsService.deleteByCodeAndType(code, type));
    }

    /*---------------------------*/
    /*     Group Overrides       */
    /*---------------------------*/

    @GetMapping("{code}/types/{type}/groups")
    public ResponseEntity<List<DashboardOverrideSummary>> getGroupsOverrideStatus(@PathVariable("code") String codeValue,
                                                                                @PathVariable("type") String type) {
        DashboardCode code = resolve(codeValue);
        return ResponseEntity.ok(overrideService.getGroupsWithOverrideStatus(code, type));
    }

    @GetMapping("{code}/types/{type}/groups/{group}/widgets")
    public ResponseEntity<List<Widget>> getEffectiveWidgets(@PathVariable("code") String codeValue,
                                                           @PathVariable("type") String type,
                                                           @PathVariable("group") String group) throws ResourceNotFoundException {
        DashboardCode code = resolve(codeValue);
        overrideService.requireGroupOfType(type, group);
        return ResponseEntity.ok(overrideService.getEffectiveWidgets(code, type, group));
    }

    @GetMapping("{code}/types/{type}/groups/{group}/widgets/overrides")
    public ResponseEntity<DashboardOverrides> getOverrides(@PathVariable("code") String codeValue,
                                                         @PathVariable("type") String type,
                                                         @PathVariable("group") String group) throws ResourceNotFoundException {
        DashboardCode code = resolve(codeValue);
        overrideService.requireGroupOfType(type, group);
        return overrideService.getByCodeAndTypeAndGroupId(code, type, group)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("{code}/types/{type}/groups/{group}/widgets/overrides")
    @PreAuthorize("hasAuthority('ADMIN') or isAdministratorOfStakeholder(#group) or isCoordinatorOfStakeholder(#group)")
    public ResponseEntity<DashboardOverrides> upsertOverrides(@PathVariable("code") String codeValue,
                                                            @PathVariable("type") String type,
                                                            @PathVariable("group") String group,
                                                            @RequestBody DashboardOverrides overrides) throws ResourceNotFoundException {
        DashboardCode code = resolve(codeValue);
        overrides.setCode(code);
        overrides.setType(type);
        overrides.setGroupId(group);
        return ResponseEntity.ok(overrideService.upsert(overrides));
    }

    @DeleteMapping("{code}/types/{type}/groups/{group}/widgets/overrides")
    @PreAuthorize("hasAuthority('ADMIN') or isAdministratorOfStakeholder(#group) or isCoordinatorOfStakeholder(#group)")
    public ResponseEntity<DashboardOverrides> deleteOverrides(@PathVariable("code") String codeValue,
                                                            @PathVariable("type") String type,
                                                            @PathVariable("group") String group) throws ResourceNotFoundException {
        DashboardCode code = resolve(codeValue);
        overrideService.requireGroupOfType(type, group);
        return ResponseEntity.ok(overrideService.deleteByCodeAndTypeAndGroupId(code, type, group));
    }
}
