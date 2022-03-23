package eu.eosc.observatory.domain;

import eu.eosc.observatory.service.Identifiable;

import java.util.List;
import java.util.stream.Collectors;

public class Coordinator implements Identifiable<String> {

    String id;
    String name;
    String type;
    List<String> members;

    public Coordinator() {}

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }
}
