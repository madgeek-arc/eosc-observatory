package eu.eosc.observatory.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eu.eosc.observatory.service.Identifiable;

import java.util.*;
import java.util.stream.Collectors;

public class UserGroup implements Identifiable<String> {

    protected String id;
    protected String name;
    protected String type;
    protected Set<String> admins;
    protected Set<String> members;

    public UserGroup() {

    }

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

    public Set<String> getAdmins() {
        return admins;
    }

    public void setAdmins(Set<String> admins) {
        this.admins = admins == null ? null : admins
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    public Set<String> getMembers() {
        return members;
    }

    public void setMembers(Set<String> members) {
        this.members = members == null ? null : members
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @JsonIgnore
    public final Set<String> getUsers() {
        Set<String> users = new TreeSet<>();
        users.addAll(Optional.ofNullable(this.admins).orElse(new HashSet<>()));
        users.addAll(Optional.ofNullable(this.members).orElse(new HashSet<>()));
        return users;
    }

    public enum GroupType {
        COUNTRY("country"),
        EOSC_SB("eosc-sb"),
        EOSC_ASSOCIATION("eosc-a"),
        CLIMATE("climate");

        private final String type;

        GroupType(String type) {
            this.type = type;
        }

        public String getKey() {
            return type;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static GroupType fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(GroupType.values())
                    .filter(v -> v.type.equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }
    }
}
