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

import eu.openaire.observatory.domain.PrivacyPolicy;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.dto.UserPrivacyPolicyInfo;
import eu.openaire.observatory.service.PrivacyPolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("privacy/policies")
public class PrivacyPolicyController extends AbstractCrudController<PrivacyPolicy> {

    private static final Logger logger = LoggerFactory.getLogger(PrivacyPolicyController.class);
    private final PrivacyPolicyService privacyPolicyService;

    @Autowired
    public PrivacyPolicyController(PrivacyPolicyService privacyPolicyService) {
        super(privacyPolicyService);
        this.privacyPolicyService = privacyPolicyService;
    }

    @GetMapping("status")
    public ResponseEntity<UserPrivacyPolicyInfo> hasAcceptedPolicy(@RequestParam(value = "type") String type, @Parameter(hidden = true) Authentication authentication) {
        UserPrivacyPolicyInfo info = new UserPrivacyPolicyInfo();
        PrivacyPolicy policy = privacyPolicyService.getLatestByType(type);
        if (policy != null) {
            info.setPrivacyPolicy(policy);
            info.setAccepted(privacyPolicyService.hasAcceptedPolicy(policy.getId(), User.getId(authentication)));
        } else {
            logger.warn("There is no Privacy Policy for [type={}]", type);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(info, HttpStatus.OK);
    }
}
