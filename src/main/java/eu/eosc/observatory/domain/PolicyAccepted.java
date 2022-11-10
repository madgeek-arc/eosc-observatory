package eu.eosc.observatory.domain;


public class PolicyAccepted {
    String id;
    long time;

    public PolicyAccepted() {
        // no-arg constructor
    }

    public PolicyAccepted(String id, long time) {
        this.id = id;
        this.time = time;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTime() {
        return time;
    }

    public void setAcceptedDate(long time) {
        this.time = time;
    }
}
