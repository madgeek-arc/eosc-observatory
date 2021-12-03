package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.User;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;

public interface UserService extends CrudItemService<User> {

    void updateUserConsent(String id, boolean consent);

    void updateUserInfo(Authentication authentication);

    void purge(String id) throws ResourceNotFoundException;
}
