package eu.eosc.observatory.dto;

public class Modification {

    Object from;
    Object to;

    public Modification() {
    }

    public Modification(Object from, Object to) {
        this.from = from;
        this.to = to;
    }

    public Object getFrom() {
        return from;
    }

    public void setFrom(Object from) {
        this.from = from;
    }

    public Object getTo() {
        return to;
    }

    public void setTo(Object to) {
        this.to = to;
    }
}
