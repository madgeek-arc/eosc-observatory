package eu.openaire.observatory.controller;

import eu.openaire.observatory.domain.DefaultIndicators;
import eu.openaire.observatory.domain.Indicator;
import eu.openaire.observatory.domain.StakeholderIndicatorsOverride;
import eu.openaire.observatory.dto.StakeholderIndicatorsSummary;
import eu.openaire.observatory.service.DefaultIndicatorsService;
import eu.openaire.observatory.service.StakeholderIndicatorsService;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class IndicatorsController {

    private final DefaultIndicatorsService defaultIndicatorsService;
    private final StakeholderIndicatorsService stakeholderIndicatorsService;

    public IndicatorsController(DefaultIndicatorsService defaultIndicatorsService,
                                StakeholderIndicatorsService stakeholderIndicatorsService) {
        this.defaultIndicatorsService = defaultIndicatorsService;
        this.stakeholderIndicatorsService = stakeholderIndicatorsService;
    }

    /*---------------------------*/
    /*     Default Indicators    */
    /*---------------------------*/

    @GetMapping("indicators/defaults/{type}")
    public ResponseEntity<DefaultIndicators> getDefaults(@PathVariable("type") String type) {
        return defaultIndicatorsService.getByType(type)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("indicators/defaults")
    @PreAuthorize("hasAuthority('ADMIN') or isCoordinatorOfType(#defaults.getType()) or isAdministratorOfType(#defaults.getType())")
    public ResponseEntity<DefaultIndicators> createDefaults(@RequestBody DefaultIndicators defaults) {
        return new ResponseEntity<>(defaultIndicatorsService.add(defaults), HttpStatus.CREATED);
    }

    @PutMapping("indicators/defaults/{type}")
    @PreAuthorize("hasAuthority('ADMIN') or isCoordinatorOfType(#type) or isAdministratorOfType(#type)")
    public ResponseEntity<DefaultIndicators> updateDefaults(@PathVariable("type") String type,
                                                            @RequestBody DefaultIndicators defaults) throws ResourceNotFoundException {
        return ResponseEntity.ok(defaultIndicatorsService.updateByType(type, defaults));
    }

    @DeleteMapping("indicators/defaults/{type}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<DefaultIndicators> deleteDefaults(@PathVariable("type") String type) throws ResourceNotFoundException {
        return ResponseEntity.ok(defaultIndicatorsService.deleteByType(type));
    }

    /*---------------------------*/
    /*   Stakeholder Indicators  */
    /*---------------------------*/

    @GetMapping("stakeholders/types/{type}/indicators/overrides")
    public ResponseEntity<List<StakeholderIndicatorsSummary>> getStakeholdersOverrideStatus(@PathVariable("type") String type) {
        return ResponseEntity.ok(stakeholderIndicatorsService.getStakeholdersWithOverrideStatus(type));
    }

    @GetMapping("stakeholders/{stakeholderId}/indicators")
    public ResponseEntity<List<Indicator>> getEffectiveIndicators(@PathVariable("stakeholderId") String stakeholderId) {
        return ResponseEntity.ok(stakeholderIndicatorsService.getEffectiveIndicators(stakeholderId));
    }

    @GetMapping("stakeholders/{stakeholderId}/indicators/overrides")
    public ResponseEntity<StakeholderIndicatorsOverride> getOverrides(@PathVariable("stakeholderId") String stakeholderId) {
        return stakeholderIndicatorsService.getByStakeholderId(stakeholderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("stakeholders/{stakeholderId}/indicators/overrides")
    @PreAuthorize("hasAuthority('ADMIN') or isStakeholderManager(#stakeholderId)")
    public ResponseEntity<StakeholderIndicatorsOverride> upsertOverrides(@PathVariable("stakeholderId") String stakeholderId,
                                                                         @RequestBody StakeholderIndicatorsOverride overrides) throws ResourceNotFoundException {
        overrides.setStakeholderId(stakeholderId);
        return ResponseEntity.ok(stakeholderIndicatorsService.upsert(overrides));
    }

    @DeleteMapping("stakeholders/{stakeholderId}/indicators/overrides")
    @PreAuthorize("hasAuthority('ADMIN') or isStakeholderManager(#stakeholderId)")
    public ResponseEntity<StakeholderIndicatorsOverride> deleteOverrides(@PathVariable("stakeholderId") String stakeholderId) throws ResourceNotFoundException {
        return ResponseEntity.ok(stakeholderIndicatorsService.deleteByStakeholderId(stakeholderId));
    }
}
