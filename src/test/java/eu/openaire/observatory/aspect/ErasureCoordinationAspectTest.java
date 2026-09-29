package eu.openaire.observatory.aspect;

import eu.openaire.observatory.controller.AdministratorController;
import eu.openaire.observatory.controller.CoordinatorController;
import eu.openaire.observatory.controller.StakeholderController;
import eu.openaire.observatory.service.UserServiceImpl;
import eu.openaire.observatory.service.StakeholderServiceImpl;
import eu.openaire.observatory.service.CoordinatorServiceImpl;
import eu.openaire.observatory.service.Identifiable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import gr.uoa.di.madgik.registry.exception.ResourceException;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ErasureCoordinationAspectTest {
    @Test
    void purgeWaitsForExistingGroupWriteAndRejectsNewWritesUntilFinished() throws Throwable {
        var aspect = new ErasureCoordinationAspect();
        var groupEntered = new CountDownLatch(1);
        var releaseGroup = new CountDownLatch(1);
        var purgeEntered = new CountDownLatch(1);
        var releasePurge = new CountDownLatch(1);
        var groupCall = mock(ProceedingJoinPoint.class);
        when(groupCall.proceed()).thenAnswer(ignored -> {
            groupEntered.countDown();
            assertTrue(releaseGroup.await(5, TimeUnit.SECONDS));
            return null;
        });
        var purgeCall = mock(ProceedingJoinPoint.class);
        when(purgeCall.proceed()).thenAnswer(ignored -> {
            purgeEntered.countDown();
            assertTrue(releasePurge.await(5, TimeUnit.SECONDS));
            return null;
        });
        try (var executor = Executors.newFixedThreadPool(2)) {
            try {
                var group = executor.submit(() -> invoke(() -> aspect.groupWrite(groupCall)));
                assertTrue(groupEntered.await(5, TimeUnit.SECONDS));
                var purge = executor.submit(() -> invoke(() -> aspect.purge(purgeCall)));
                assertThrows(TimeoutException.class, () -> purge.get(100, TimeUnit.MILLISECONDS));
                verify(purgeCall, never()).proceed();
                releaseGroup.countDown();
                group.get(5, TimeUnit.SECONDS);
                assertTrue(purgeEntered.await(5, TimeUnit.SECONDS));

                var rejected = mock(ProceedingJoinPoint.class);
                var error = assertThrows(ResourceException.class, () -> aspect.groupWrite(rejected));
                assertEquals(HttpStatus.CONFLICT, error.getStatus());
                verify(rejected, never()).proceed();

                releasePurge.countDown();
                purge.get(5, TimeUnit.SECONDS);
                aspect.groupWrite(rejected);
                verify(rejected).proceed();
            } finally {
                releaseGroup.countDown();
                releasePurge.countDown();
            }
        }
    }

    @Test
    void failedPurgeReleasesGroupWrites() throws Throwable {
        var aspect = new ErasureCoordinationAspect();
        var purge = mock(ProceedingJoinPoint.class);
        when(purge.proceed()).thenThrow(new IllegalStateException("failed"));
        assertThrows(IllegalStateException.class, () -> aspect.purge(purge));
        // A separate thread prevents reentrancy from concealing an unreleased write lock.
        try (var executor = Executors.newSingleThreadExecutor()) {
            var group = mock(ProceedingJoinPoint.class);
            executor.submit(() -> invoke(() -> aspect.groupWrite(group))).get(5, TimeUnit.SECONDS);
            verify(group).proceed();
        }
    }

    @Test
    void pointcutsCoverActualGroupWriteEndpointsAndPurge() throws Exception {
        var group = pointcut("groupWrite");
        for (var controller : new Class<?>[]{StakeholderController.class, CoordinatorController.class,
                AdministratorController.class}) {
            for (var method : controller.getDeclaredMethods()) {
                boolean writes = method.isAnnotationPresent(PostMapping.class)
                        || method.isAnnotationPresent(PutMapping.class)
                        || method.isAnnotationPresent(PatchMapping.class)
                        || method.isAnnotationPresent(DeleteMapping.class);
                assertEquals(writes, group.matches(method, controller), method.toString());
            }
        }
        assertTrue(group.matches(StakeholderServiceImpl.class.getMethod("addMember", String.class, String.class),
                StakeholderServiceImpl.class));
        assertTrue(group.matches(StakeholderServiceImpl.class.getMethod("removeAdmin", String.class, String.class),
                StakeholderServiceImpl.class));
        assertTrue(group.matches(CoordinatorServiceImpl.class.getMethod("update", String.class, Identifiable.class),
                CoordinatorServiceImpl.class));
        assertTrue(pointcut("purge").matches(UserServiceImpl.class.getMethod("purge", String.class),
                UserServiceImpl.class));
    }

    @Test
    void purgeCanCallGroupServicesWithoutRejectingItsOwnCleanup() throws Throwable {
        var aspect = new ErasureCoordinationAspect();
        var purge = mock(ProceedingJoinPoint.class);
        var cleanup = mock(ProceedingJoinPoint.class);
        when(purge.proceed()).thenAnswer(ignored -> aspect.groupWrite(cleanup));
        aspect.purge(purge);
        verify(cleanup).proceed();
    }

    private static AspectJExpressionPointcut pointcut(String advice) throws Exception {
        var pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(ErasureCoordinationAspect.class.getMethod(advice, ProceedingJoinPoint.class)
                .getAnnotation(Around.class).value());
        return pointcut;
    }

    private interface Invocation { Object run() throws Throwable; }

    private static Object invoke(Invocation invocation) {
        try {
            return invocation.run();
        } catch (Throwable error) {
            throw new CompletionException(error);
        }
    }
}
