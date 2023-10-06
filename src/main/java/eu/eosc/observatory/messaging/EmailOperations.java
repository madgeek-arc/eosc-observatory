package eu.eosc.observatory.messaging;

import gr.athenarc.messaging.dto.ThreadDTO;

public interface EmailOperations {

    void sendEmails(ThreadDTO threadDTO);

    void sendReminder(ThreadDTO threadDTO);

    void sendReport(ThreadDTO threadDTO);

}
