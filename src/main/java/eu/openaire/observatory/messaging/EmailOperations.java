/*
 * Copyright 2021-2025 OpenAIRE AMKE
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
package eu.openaire.observatory.messaging;

import gr.athenarc.messaging.dto.ThreadDTO;

public interface EmailOperations {

    /**
     * Sends email messages for the last {@link ThreadDTO#getMessages() message} in the {@link ThreadDTO topic}.
     * @param threadDTO
     */
    void sendEmails(ThreadDTO threadDTO);

    /**
     * Sends reminder messages regarding a {@link ThreadDTO topic}.
     * @param threadDTO
     */
    void sendReminder(ThreadDTO threadDTO);

    void sendReport(ThreadDTO threadDTO);

}
