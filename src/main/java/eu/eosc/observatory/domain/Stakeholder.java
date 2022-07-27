package eu.eosc.observatory.domain;

import eu.eosc.observatory.service.Identifiable;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Stakeholder implements Identifiable<String> {

    String id; // sh-type-[country/associationMember]
    String name;
    String type;
    String subType;
    String country; // 2-letter code
    String associationMember; // will create list at some point
    boolean mandated;
    Set<String> managers;
    Set<String> contributors;

    public Stakeholder() {
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

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getAssociationMember() {
        return associationMember;
    }

    public void setAssociationMember(String associationMember) {
        this.associationMember = associationMember;
    }

    public boolean isMandated() {
        return mandated;
    }

    public void setMandated(boolean mandated) {
        this.mandated = mandated;
    }

    public Set<String> getManagers() {
        return managers;
    }

    public void setManagers(Set<String> managers) {
        this.managers = managers == null ? null : managers
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    public Set<String> getContributors() {
        return contributors;
    }

    public void setContributors(Set<String> contributors) {
        this.contributors = contributors == null ? null : contributors
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    public enum StakeholderType {
        COUNTRY("country"),
        ASSOCIATION("association");

        private final String type;

        StakeholderType(String type) {
            this.type = type;
        }

        public String getKey() {
            return type;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static StakeholderType fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(StakeholderType.values())
                    .filter(v -> v.type.equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stakeholder)) return false;
        Stakeholder that = (Stakeholder) o;
        return id.equals(that.id) && Objects.equals(name, that.name) && Objects.equals(type, that.type) && Objects.equals(subType, that.subType) && Objects.equals(country, that.country) && Objects.equals(associationMember, that.associationMember) && Objects.equals(managers, that.managers) && Objects.equals(contributors, that.contributors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, type, subType, country, associationMember, managers, contributors);
    }
}
