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

import eu.openaire.observatory.domain.SurveyTypeSettings;
import eu.openaire.observatory.service.SurveyTypeSettingsService;
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
public class SurveyTypeSettingsController {

    private final SurveyTypeSettingsService surveyTypeSettingsService;

    public SurveyTypeSettingsController(SurveyTypeSettingsService surveyTypeSettingsService) {
        this.surveyTypeSettingsService = surveyTypeSettingsService;
    }

    @GetMapping("/surveys/type/{type}/notification-settings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SurveyTypeSettings> getByType(@PathVariable("type") String type) {
        return ResponseEntity.ok(surveyTypeSettingsService.getByType(type));
    }

    @GetMapping("/surveys/type/notification-settings")
    @BrowseParameters
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Browsing<SurveyTypeSettings>> getAll(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter filter = FacetFilter.from(allRequestParams);
        return ResponseEntity.ok(surveyTypeSettingsService.getAll(filter));
    }

    @PutMapping("/surveys/type/{type}/notification-settings")
    @PreAuthorize("hasAuthority('ADMIN') or isCoordinatorOfType(#type)")
    public ResponseEntity<SurveyTypeSettings> upsert(@PathVariable("type") String type,
                                                     @RequestBody SurveyTypeSettings settings) {
        settings.setSurveyType(type);
        return ResponseEntity.ok(surveyTypeSettingsService.upsert(settings));
    }

    @DeleteMapping("/surveys/type/{type}/notification-settings")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<SurveyTypeSettings> delete(@PathVariable("type") String type) throws ResourceNotFoundException {
        return ResponseEntity.ok(surveyTypeSettingsService.deleteByType(type));
    }
}
