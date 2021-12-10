//package eu.eosc.observatory.configuration.security;
//
//import eu.eosc.observatory.domain.Stakeholder;
//import eu.eosc.observatory.domain.User;
//import eu.eosc.observatory.service.StakeholderService;
//import eu.eosc.observatory.service.UserService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.expression.SecurityExpressionRoot;
//import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
//import org.springframework.security.core.Authentication;
//
//import java.util.HashSet;
//import java.util.Set;
//
//public class CustomMethodSecurityExpressionRoot
//        extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {
//
//    private final UserService userService;
//    private final StakeholderService stakeholderService;
//
//    private Object returnObject;
//    private Object filterObject;
//
//    public CustomMethodSecurityExpressionRoot(Authentication authentication, UserService userService, StakeholderService stakeholderService) {
//        super(authentication);
//        this.userService = userService;
//        this.stakeholderService = stakeholderService;
//    }
//
//    public boolean isStakeholderMember(String stakeholderId) {
//        User user = userService.get(User.getId(this.authentication));
//        Stakeholder stakeholder = stakeholderService.get(stakeholderId);
//        Set<String> emails = new HashSet<>();
//        if (stakeholder.getContributors() != null) {
//            emails.addAll(stakeholder.getContributors());
//        }
//        if (stakeholder.getManagers() != null) {
//            emails.addAll(stakeholder.getManagers());
//        }
//        return emails.contains(user.getId());
//    }
//
//    public boolean isStakeholderManager(String stakeholderId) {
//        User user = userService.get(User.getId(this.authentication));
//        Stakeholder stakeholder = stakeholderService.get(stakeholderId);
//        return stakeholder.getManagers() != null && stakeholder.getManagers().contains(user.getId());
//    }
//
//    @Override
//    public void setFilterObject(Object o) {
//        this.filterObject = o;
//    }
//
//    @Override
//    public Object getFilterObject() {
//        return filterObject;
//    }
//
//    @Override
//    public void setReturnObject(Object o) {
//        if (o != null) {
//            if (o instanceof ResponseEntity<?>) {
//                o = ((ResponseEntity<?>) o).getBody();
//            }
//        }
//        this.returnObject = o;
//    }
//
//    @Override
//    public Object getReturnObject() {
//        return returnObject;
//    }
//
//    @Override
//    public Object getThis() {
//        return this;
//    }
//}
