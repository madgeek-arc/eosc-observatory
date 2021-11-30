package eu.eosc.observatory.user;

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

@RestController
@RequestMapping("user")
public class UserController {

    private static final Logger logger = LogManager.getLogger(UserController.class);

    @Autowired
    public UserController() {

    }

    @GetMapping("info")
    public ResponseEntity<User> userInfo(@ApiIgnore Authentication authentication) {
        User user = User.of(authentication);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }
}
