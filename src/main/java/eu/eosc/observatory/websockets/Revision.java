package eu.eosc.observatory.websockets;

public class Revision {

    private String field;
    private Object value;

    public Revision() {
        // no-arg constructor
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
