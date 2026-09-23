/*
 * Copyright 2021-2026 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.openaire.observatory.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import gr.uoa.di.madgik.registry.exception.ResourceException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Single-backend coordination of group writes and erasure. Service advice also covers invitation
 * acceptance and other internal callers. Controller advice holds the lock across the entire
 * group mutation request, including reads that precede writes.
 * Operators must still pause administrative edits and discard stale forms during erasure.
 * Direct database writes and other backend instances are outside this lock's scope.
 */
@Aspect
@Component
@Order(0)
public class ErasureCoordinationAspect {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    @Around("((within(eu.openaire.observatory.controller.StakeholderController) || "
            + "within(eu.openaire.observatory.controller.CoordinatorController) || "
            + "within(eu.openaire.observatory.controller.AdministratorController)) && "
            + "(@annotation(org.springframework.web.bind.annotation.PostMapping) || "
            + "@annotation(org.springframework.web.bind.annotation.PutMapping) || "
            + "@annotation(org.springframework.web.bind.annotation.PatchMapping) || "
            + "@annotation(org.springframework.web.bind.annotation.DeleteMapping))) || "
            + "(target(eu.openaire.observatory.service.AbstractUserGroupService) && "
            + "(execution(public * add*(..)) || execution(public * update*(..)) || "
            + "execution(public * remove*(..)) || execution(public * delete(..)) || "
            + "execution(public * restore(..))))")
    public Object groupWrite(ProceedingJoinPoint call) throws Throwable {
        // Timed tryLock respects fairness: new requests cannot bypass a waiting purge.
        if (!lock.readLock().tryLock(0, TimeUnit.SECONDS)) {
            throw new ResourceException(
                    "User erasure is in progress. Reload the group after the operator confirms completion.",
                    HttpStatus.CONFLICT);
        }
        try {
            return call.proceed();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Around("execution(* eu.openaire.observatory.service.UserServiceImpl.purge(..))")
    public Object purge(ProceedingJoinPoint call) throws Throwable {
        lock.writeLock().lockInterruptibly();
        try {
            return call.proceed();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
