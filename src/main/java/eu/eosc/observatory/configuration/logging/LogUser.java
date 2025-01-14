package eu.eosc.observatory.configuration.logging;

import eu.eosc.observatory.domain.User;
import gr.athenarc.catalogue.config.logging.AbstractLogContextFilter;
import org.slf4j.spi.MDCAdapter;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@Component
public class LogUser extends AbstractLogContextFilter {

    @Override
    public void editMDC(MDCAdapter mdc, ServletRequest request, ServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            try {
                User user = User.of(authentication);
                mdc.put("user_info", user.toString());
            } catch (InsufficientAuthenticationException e) {
                mdc.put("user_info", authentication.toString());
            }
        }
    }
}
