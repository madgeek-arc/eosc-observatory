//package eu.eosc.observatory.configuration.security;
//
//import eu.eosc.observatory.service.StakeholderService;
//import eu.eosc.observatory.service.UserService;
//import org.aopalliance.intercept.MethodInvocation;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
//import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
//import org.springframework.security.authentication.AuthenticationTrustResolver;
//import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
//import org.springframework.security.core.Authentication;
//import org.springframework.stereotype.Component;
//
//@Component
//public class CustomMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {
//    private AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
//
//    @Autowired
//    private UserService userService;
//
//    @Autowired
//    private StakeholderService stakeholderService;
//
//    @Override
//    protected MethodSecurityExpressionOperations createSecurityExpressionRoot(Authentication authentication, MethodInvocation invocation) {
//        CustomMethodSecurityExpressionRoot root =
//                new CustomMethodSecurityExpressionRoot(authentication, userService, stakeholderService);
//        root.setPermissionEvaluator(getPermissionEvaluator());
//        root.setTrustResolver(this.trustResolver);
//        root.setRoleHierarchy(getRoleHierarchy());
//        return root;
//    }
//}
