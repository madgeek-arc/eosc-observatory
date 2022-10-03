package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.PrivacyPolicy;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.dto.UserPrivacyPolicyInfo;
import eu.eosc.observatory.service.PrivacyPolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

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
    public ResponseEntity<UserPrivacyPolicyInfo> hasAcceptedPolicy(@RequestParam(value = "type") String type, @ApiIgnore Authentication authentication) {
        UserPrivacyPolicyInfo info = new UserPrivacyPolicyInfo();
        PrivacyPolicy policy = privacyPolicyService.getLatestByType(type);
        if (policy != null) {
            info.setPrivacyPolicy(policy);
            info.setAccepted(privacyPolicyService.hasAcceptedPolicy(policy.getId(), User.of(authentication).getId()));
        } else {
            logger.warn("There is no Privacy Policy for [type={}]", type);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(info, HttpStatus.OK);
    }
}
