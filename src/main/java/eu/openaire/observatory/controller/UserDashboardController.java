package eu.openaire.observatory.controller;

import eu.openaire.observatory.domain.DashboardItem;
import eu.openaire.observatory.domain.UserDashboard;
import eu.openaire.observatory.service.UserDashboardService;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class UserDashboardController {

    private final UserDashboardService dashboardService;

    public UserDashboardController(UserDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("dashboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDashboard> getDashboard(@Parameter(hidden = true) Authentication authentication) {
        return ResponseEntity.ok(dashboardService.getOrCreateForCurrentUser(authentication));
    }

    @PutMapping("dashboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDashboard> saveDashboard(@RequestBody List<DashboardItem> items,
                                                        @Parameter(hidden = true) Authentication authentication) {
        return ResponseEntity.ok(dashboardService.save(authentication, items));
    }

    // ADMIN / SUPPORT METHODS

    @GetMapping("users/{id}/dashboard")
    @PreAuthorize("hasAuthority('ADMIN') or (isAuthenticated() and authentication.principal.getAttribute('email') == #userId)")
    public ResponseEntity<UserDashboard> getUserDashboard(@PathVariable("id") String userId) {
        return ResponseEntity.ok(dashboardService.getOrCreate(userId));
    }

    @PutMapping("users/{id}/dashboard")
    @PreAuthorize("hasAuthority('ADMIN') or (isAuthenticated() and authentication.principal.getAttribute('email') == #userId)")
    public ResponseEntity<UserDashboard> saveUserDashboard(@PathVariable("id") String userId,
                                                            @RequestBody List<DashboardItem> items,
                                                            @Parameter(hidden = true) Authentication authentication) {
        return ResponseEntity.ok(dashboardService.save(userId, authentication, items));
    }
}
