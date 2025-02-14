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

import eu.openaire.observatory.domain.IdNameTuple;
import eu.openaire.observatory.domain.UserGroup;
import eu.openaire.observatory.service.CoordinatorService;
import eu.openaire.observatory.service.StakeholderService;
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
