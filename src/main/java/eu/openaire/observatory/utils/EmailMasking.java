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

package eu.openaire.observatory.utils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Masks email addresses before they reach a log line, leaving everything else about the string
 * untouched: a non-email value (no {@code @}) is returned as-is rather than guessed at.
 */
public final class EmailMasking {

    private EmailMasking() {
        // utility class
    }

    /** Returns {@code "j*****@example.org"} for {@code "jane@example.org"}. {@code null} in, {@code null} out. */
    public static String mask(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return email;
        }
        return email.charAt(0) + "*****@" + email.substring(at + 1);
    }

    public static List<String> mask(Collection<String> emails) {
        if (emails == null) {
            return null;
        }
        return emails.stream().map(EmailMasking::mask).collect(Collectors.toList());
    }
}
