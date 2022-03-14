package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.User;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;

public interface UserService extends CrudItemService<User> {

    User getUser(String id);

    User acceptPrivacyPolicy(String policyId, Authentication authentication);

    void updateUserInfo(Authentication authentication);

    void purge(String id) throws ResourceNotFoundException;
}
