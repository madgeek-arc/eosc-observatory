package eu.openaire.observatory.permissions;

import gr.uoa.di.madgik.authorization.domain.Permission;
import gr.uoa.di.madgik.authorization.repository.PermissionRepository;
import gr.uoa.di.madgik.authorization.service.Authorization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceBridgeTest {

    @Mock
    private Authorization authorization;
    @Mock
    private PermissionRepository permissionRepository;

    private AuthorizationServiceBridge service;

    @BeforeEach
    void setUp() {
        service = new AuthorizationServiceBridge(authorization, permissionRepository);
    }

    @Test
    void addPermissionsCreatesOnlyMissingPermissions() {
        when(permissionRepository.findAllBySubjectAndActionAndObject("user-1", "read", "res-1")).thenReturn(Set.of());
        when(permissionRepository.findAllBySubjectAndActionAndObject("user-1", "write", "res-1"))
                .thenReturn(Set.of(new Permission("user-1", "write", "res-1", "group-1")));

        Set<Permission> created = service.addPermissions(
                List.of("user-1"),
                List.of("read", "write"),
                List.of("res-1"),
                "group-1"
        );

        assertEquals(1, created.size());
        Permission permission = created.iterator().next();
        assertEquals("user-1", permission.getSubject());
        assertEquals("read", permission.getAction());
        assertEquals("res-1", permission.getObject());
        verify(permissionRepository).saveAll(created);
    }

    @Test
    void removePermissionsWithGroupDeletesEveryMatchingPermission() {
        when(permissionRepository.findAllBySubjectAndActionAndObjectAndSubjectGroup("user-1", "read", "res-1", "group-1"))
                .thenReturn(Set.of(new Permission("user-1", "read", "res-1", "group-1")));
        when(permissionRepository.findAllBySubjectAndActionAndObjectAndSubjectGroup("user-1", "read", "res-2", "group-1"))
                .thenReturn(Set.of(new Permission("user-1", "read", "res-2", "group-1")));

        service.removePermissions(List.of("user-1"), List.of("read"), List.of("res-1", "res-2"), "group-1");

        verify(permissionRepository, times(2)).delete(any(Permission.class));
    }

    @Test
    void getResourcePermissionsMapsEveryRequestedId() {
        when(authorization.whatCan("user-1", "res-1")).thenReturn(Set.of(
                new Permission("user-1", "read", "res-1", null),
                new Permission("user-1", "write", "res-1", null)
        ));
        when(authorization.whatCan("user-1", "res-2")).thenReturn(Set.of(
                new Permission("user-1", "read", "res-2", null)
        ));

        var permissions = service.getResourcePermissions("user-1", List.of("res-1", "res-2"));

        assertEquals(2, permissions.size());
        assertEquals(Set.of("read", "write"), permissions.get(0).getPermissions());
        assertEquals(Set.of("read"), permissions.get(1).getPermissions());
    }
}
