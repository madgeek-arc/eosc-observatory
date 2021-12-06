package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.domain.UserInfo;
import eu.eosc.observatory.service.CrudItemService;
import eu.eosc.observatory.service.UserService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("user")
public class UserController {

    private static final Logger logger = LogManager.getLogger(UserController.class);

    private final UserService userService;
    private final CrudItemService<Stakeholder> stakeholderService;

    @Autowired
    public UserController(UserService userService,
                          CrudItemService<Stakeholder> stakeholderService) {
        this.userService = userService;
        this.stakeholderService = stakeholderService;
    }

    @GetMapping("info")
    public ResponseEntity<UserInfo> userInfo(@ApiIgnore Authentication authentication) {
        User user = userService.get(User.getId(authentication));
        UserInfo info = new UserInfo();
        info.setUser(user);
        info.setMemberOf(new ArrayList<>());

        info.getMemberOf().addAll(getStakeholdersWithFilter("managers", user.getEmail()));
        info.getMemberOf().addAll(getStakeholdersWithFilter("contributors", user.getEmail()));

        return new ResponseEntity<>(info, HttpStatus.OK);
    }

    @PatchMapping("/consent")
    public ResponseEntity<Void> setConsent(@RequestParam(value = "consent", defaultValue = "false") boolean consent, @ApiIgnore Authentication authentication) throws ResourceNotFoundException {
        userService.updateUserConsent(User.getId(authentication), consent);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private List<Stakeholder> getStakeholdersWithFilter(String key, String value) {
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);
        filter.addFilter(key, value);
        Browsing<Stakeholder> results = stakeholderService.getAll(filter);
        return results.getResults()
                .stream()
//                .map(stakeholder -> new IdNameTuple(stakeholder.getId(), stakeholder.getName()))
                .collect(Collectors.toList());
    }
}
