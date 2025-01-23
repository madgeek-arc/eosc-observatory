package eu.eosc.observatory.domain;

import java.util.Objects;

public class AssociationStakeholder extends Stakeholder {

    String associationMember;
    boolean mandated;

    public AssociationStakeholder() {
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
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AssociationStakeholder that = (AssociationStakeholder) o;
        return mandated == that.mandated && Objects.equals(associationMember, that.associationMember);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), associationMember, mandated);
    }
}
