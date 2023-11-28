package eu.eosc.observatory.messaging;

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
