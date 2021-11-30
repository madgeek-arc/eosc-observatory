package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.IdNameTuple;
import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.domain.UserInfo;
import eu.eosc.observatory.service.CrudItemService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("user")
public class UserController {

    private static final Logger logger = LogManager.getLogger(UserController.class);

    private final CrudItemService<Stakeholder> stakeholderService;

    @Autowired
    public UserController(CrudItemService<Stakeholder> stakeholderService) {
        this.stakeholderService = stakeholderService;
    }

    @GetMapping("info")
    public ResponseEntity<UserInfo> userInfo(@ApiIgnore Authentication authentication) {
        User user = User.of(authentication);
        UserInfo info = new UserInfo();
        info.setUser(user);
        info.setMemberOf(new ArrayList<>());

        info.getMemberOf().addAll(getStakeholdersWithFilter("managers", user.getEmail()));
        info.getMemberOf().addAll(getStakeholdersWithFilter("contributors", user.getEmail()));

        return new ResponseEntity<>(info, HttpStatus.OK);
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
