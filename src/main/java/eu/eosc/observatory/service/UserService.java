package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.domain.UserInfo;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;

public interface UserService extends CrudService<User> {

    User getUser(String id);

    UserInfo getUserInfo(String id);

    UserInfo getUserInfo(Authentication authentication);

    User acceptPrivacyPolicy(String policyId, Authentication authentication);

    void updateUserDetails(Authentication authentication);

    void purge(String id) throws ResourceNotFoundException;
}
