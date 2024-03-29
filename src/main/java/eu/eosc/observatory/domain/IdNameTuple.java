package eu.eosc.observatory.domain;

public class IdNameTuple {

    String id;
    String name;

    public IdNameTuple() {
        // no-arg constructor
    }

    public IdNameTuple(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
