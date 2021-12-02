package eu.eosc.observatory.dto;

import eu.eosc.observatory.domain.User;

import java.util.List;

public class StakeholderMembers {

    List<User> contributors;
    List<User> managers;

    public StakeholderMembers() {
    }

    public StakeholderMembers(List<User> contributors, List<User> managers) {
        this.contributors = contributors;
        this.managers = managers;
    }

    public List<User> getContributors() {
        return contributors;
    }

    public void setContributors(List<User> contributors) {
        this.contributors = contributors;
    }

    public List<User> getManagers() {
        return managers;
    }

    public void setManagers(List<User> managers) {
        this.managers = managers;
    }
}
