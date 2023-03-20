package eu.eosc.observatory.dto;

import java.util.LinkedList;
import java.util.List;

public class Node {

    String name;
    Modification modification = null;
    List<Node> fields = new LinkedList<>();

    public Node() {
    }

    public Node(String name, Modification modification, List<Node> fields) {
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

    public List<Node> getFields() {
        return fields;
    }

    public void setFields(List<Node> fields) {
        this.fields = fields;
    }
}
