package eu.eosc.observatory.domain;

import java.util.Objects;

public class Stakeholder extends UserGroup {

    String subType;
    String country; // 2-letter code
    String associationMember; // will create list at some point
    boolean mandated;

    public Stakeholder() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stakeholder)) return false;
        Stakeholder that = (Stakeholder) o;
        return id.equals(that.id) && Objects.equals(name, that.name) && Objects.equals(type, that.type) && Objects.equals(subType, that.subType) && Objects.equals(country, that.country) && Objects.equals(associationMember, that.associationMember) && Objects.equals(admins, that.admins) && Objects.equals(members, that.members);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, type, subType, country, associationMember, admins, members);
    }
}
