package eu.eosc.observatory.dto;

import eu.eosc.observatory.domain.Stakeholder;

public class StakeholderInfo {

    String id;
    String name;
    String type;
    String subType;
    String country;
    String associationMember;

    public StakeholderInfo() {}

    public static StakeholderInfo of(Stakeholder stakeholder) {
        StakeholderInfo info = new StakeholderInfo();
        info.setId(stakeholder.getId());
        info.setName(stakeholder.getName());
        info.setType(stakeholder.getType());
        info.setSubType(stakeholder.getSubType());
        info.setCountry(stakeholder.getCountry());
        info.setAssociationMember(stakeholder.getAssociationMember());
        return info;
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
}
