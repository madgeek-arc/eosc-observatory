package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.IdNameTuple;
import eu.eosc.observatory.domain.UserGroup;
import eu.eosc.observatory.service.CoordinatorService;
import eu.eosc.observatory.service.StakeholderService;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class GroupsController {

    private final CoordinatorService coordinatorService;
    private final StakeholderService stakeholderService;

    public GroupsController(CoordinatorService coordinatorService, StakeholderService stakeholderService) {
        this.coordinatorService = coordinatorService;
        this.stakeholderService = stakeholderService;
    }

    @GetMapping("groups")
    public Map<String, List<IdNameTuple>> getGroups(@RequestParam(defaultValue = "") String type) {
        Map<String, List<IdNameTuple>> groupsMap = new HashMap<>();
        FacetFilter stakeholderFilter = new FacetFilter();
        stakeholderFilter.setQuantity(10000);
        stakeholderFilter.addFilter("type", type);
        stakeholderFilter.addOrderBy("name", "asc");
        groupsMap.put("Country Data", stakeholderService.getAll(stakeholderFilter).getResults().stream().map(stakeholder -> new IdNameTuple(stakeholder.getId(), stakeholder.getName())).collect(Collectors.toList()));
        FacetFilter coordinatorsFilter = new FacetFilter();
        coordinatorsFilter.setQuantity(10000);
        coordinatorsFilter.addFilter("type", type);
        coordinatorsFilter.addOrderBy("name", "asc");
        groupsMap.put("Survey Improvements", coordinatorService.getAll(coordinatorsFilter).getResults().stream().map(coordinator -> new IdNameTuple(coordinator.getId(), coordinator.getName())).collect(Collectors.toList()));
        groupsMap.put("Dashboard Improvement", List.of(new IdNameTuple("admin", "Administrators")));
        return groupsMap;
    }

    @GetMapping("groups/types")
    public List<String> getUserGroupTypes() {
        return Arrays.stream(UserGroup.GroupType.values()).map(UserGroup.GroupType::getKey).toList();
    }
}
