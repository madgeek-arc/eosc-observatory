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

package eu.openaire.observatory.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/** Single-instance coordination. Every holder and waiter retains the same lock until release. */
@Component
public class SurveyAnswerLocks {
    private final ConcurrentHashMap<String, Entry> locks = new ConcurrentHashMap<>();

    public static String answerId(String key) {
        return key.startsWith(RedisCacheService.PREFIX) ? key.substring(RedisCacheService.PREFIX.length()) : key;
    }

    public Guard acquire(String key) {
        String id = answerId(key);
        Entry entry = locks.compute(id, (ignored, existing) -> {
            Entry retained = existing == null ? new Entry() : existing;
            retained.references++;
            return retained;
        });
        entry.lock.lock();
        return new Guard(id, entry);
    }

    private static class Entry {
        private final ReentrantLock lock = new ReentrantLock();
        // Accessed only inside ConcurrentHashMap.compute for this key.
        private int references;
    }

    public final class Guard implements AutoCloseable {
        private final String id;
        private final Entry entry;
        private boolean closed;

        private Guard(String id, Entry entry) {
            this.id = id;
            this.entry = entry;
        }

        @Override
        public void close() {
            if (!closed) {
                entry.lock.unlock();
                locks.compute(id, (ignored, retained) -> --retained.references == 0 ? null : retained);
                closed = true;
            }
        }
    }
}
