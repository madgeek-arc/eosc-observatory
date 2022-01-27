package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.PrivacyPolicy;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.domain.UserAcceptedPolicy;
import eu.eosc.observatory.dto.UserPrivacyPolicyInfo;
import eu.eosc.observatory.service.CrudItemService;
import eu.eosc.observatory.service.PrivacyPolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Date;

@RestController
@RequestMapping("privacy/policies")
public class PrivacyPolicyController extends AbstractCrudController<PrivacyPolicy> {

    private final PrivacyPolicyService privacyPolicyService;
    private final CrudItemService<UserAcceptedPolicy> userAcceptedPolicyService;

    @Autowired
    public PrivacyPolicyController(PrivacyPolicyService privacyPolicyService,
                                   CrudItemService<UserAcceptedPolicy> userAcceptedPolicyService) {
        super(privacyPolicyService);
        this.privacyPolicyService = privacyPolicyService;
        this.userAcceptedPolicyService = userAcceptedPolicyService;
    }

    @GetMapping("status")
    public ResponseEntity<UserPrivacyPolicyInfo> hasAcceptedPolicy(@RequestParam(value = "type") String type, @ApiIgnore Authentication authentication) {
        UserPrivacyPolicyInfo info = new UserPrivacyPolicyInfo();
        PrivacyPolicy policy = privacyPolicyService.getLatestByType(type);
        info.setPrivacyPolicy(policy);
        info.setAccepted(privacyPolicyService.hasAcceptedPolicy(policy.getId(), User.of(authentication).getId()));
        return new ResponseEntity<>(info, HttpStatus.OK);
    }

    @PostMapping("{policyId}/accepted")
    public ResponseEntity<UserAcceptedPolicy> acceptPolicy(@PathVariable("policyId") String policyId, @ApiIgnore Authentication authentication) {
        privacyPolicyService.get(policyId);
        UserAcceptedPolicy acceptedPolicy = userAcceptedPolicyService.add(new UserAcceptedPolicy(User.of(authentication).getId(), policyId, new Date()));
        return new ResponseEntity<>(acceptedPolicy, HttpStatus.OK);
    }
}
