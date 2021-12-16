package eu.eosc.observatory.dto;

import eu.eosc.observatory.domain.User;

import java.util.Set;

public class StakeholderMembers {

    Set<User> contributors;
    Set<User> managers;

    public StakeholderMembers() {
    }

    public StakeholderMembers(Set<User> contributors, Set<User> managers) {
        this.contributors = contributors;
        this.managers = managers;
    }

    public Set<User> getContributors() {
        return contributors;
    }

    public void setContributors(Set<User> contributors) {
        this.contributors = contributors;
    }

    public Set<User> getManagers() {
        return managers;
    }

    public void setManagers(Set<User> managers) {
        this.managers = managers;
    }
}
