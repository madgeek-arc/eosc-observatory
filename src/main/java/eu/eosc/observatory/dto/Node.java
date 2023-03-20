package eu.eosc.observatory.dto;

import java.util.Map;

public class Node {

    String name;
    Modification modification;
    Map<String, Node> fields;

    public Node() {
    }

    public Node(String name, Modification modification, Map<String, Node> fields) {
        this.name = name;
        this.modification = modification;
        this.fields = fields;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Modification getModification() {
        return modification;
    }

    public void setModification(Modification modification) {
        this.modification = modification;
    }

    public Map<String, Node> getFields() {
        return fields;
    }

    public void setFields(Map<String, Node> fields) {
        this.fields = fields;
    }
}
