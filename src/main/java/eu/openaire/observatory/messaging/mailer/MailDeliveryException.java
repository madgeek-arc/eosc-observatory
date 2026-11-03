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

package eu.openaire.observatory.messaging.mailer;

/**
 * Thrown by {@link ResilientMailer} when an email could not be handed to the mailer service: either
 * every retry attempt failed with a transient error, or the first attempt failed with a
 * non-retryable one (e.g. a 4xx response, a misconfigured host).
 *
 * <p>Unchecked so it flows through the existing {@code catch (Exception)} guards in the mail-sending
 * services without changing any method signatures.
 */
public class MailDeliveryException extends RuntimeException {

    public MailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
