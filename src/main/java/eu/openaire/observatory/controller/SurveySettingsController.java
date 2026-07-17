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

import eu.openaire.observatory.domain.SurveySettings;
import eu.openaire.observatory.service.SurveySettingsService;
import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class SurveySettingsController {

    private final SurveySettingsService surveySettingsService;

    public SurveySettingsController(SurveySettingsService surveySettingsService) {
        this.surveySettingsService = surveySettingsService;
    }

    @GetMapping("/surveys/types/{type}/settings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SurveySettings> getByType(@PathVariable("type") String type) {
        return ResponseEntity.ok(surveySettingsService.getByType(type));
    }

    @GetMapping("/surveys/settings")
    @BrowseParameters
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Browsing<SurveySettings>> getAll(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter filter = FacetFilter.from(allRequestParams);
        return ResponseEntity.ok(surveySettingsService.getAll(filter));
    }

    @PutMapping("/surveys/types/{type}/settings")
    @PreAuthorize("hasAuthority('ADMIN') or isCoordinatorOfType(#type)")
    public ResponseEntity<SurveySettings> upsert(@PathVariable("type") String type,
                                                 @RequestBody SurveySettings settings) {
        settings.setSurveyType(type);
        return ResponseEntity.ok(surveySettingsService.upsert(settings));
    }

    @DeleteMapping("/surveys/types/{type}/settings")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<SurveySettings> delete(@PathVariable("type") String type) throws ResourceNotFoundException {
        return ResponseEntity.ok(surveySettingsService.deleteByType(type));
    }
}
