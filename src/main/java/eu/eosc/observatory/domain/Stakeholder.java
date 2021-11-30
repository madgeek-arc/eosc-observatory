package eu.eosc.observatory.domain;

import eu.eosc.observatory.service.Identifiable;

import java.util.Arrays;
import java.util.List;

public class Stakeholder implements Identifiable<String> {

    String id; // sh-type-[country/associationMember]
    String name;
    String type;
    String subType;
    String country; // 2-letter code
    String associationMember; // will create list at some point
    List<String> managers;
    List<String> contributors;

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

    public List<String> getManagers() {
        return managers;
    }

    public void setManagers(List<String> managers) {
        this.managers = managers;
    }

    public List<String> getContributors() {
        return contributors;
    }

    public void setContributors(List<String> contributors) {
        this.contributors = contributors;
    }

    public enum StakeholderType {
        COUNTRY("country"),
        ASSOCIATION_MEMBER("assosiation_member");

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
}
