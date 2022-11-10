package eu.eosc.observatory.domain;

import java.util.Set;

public class UserInfo {
    private User user;
    private Set<Stakeholder> stakeholders;
    private Set<Coordinator> coordinators;

    public UserInfo() {
        // no-arg constructor
    }

    public UserInfo(User user, Set<Stakeholder> stakeholders, Set<Coordinator> coordinators) {
        this.user = user;
        this.stakeholders = stakeholders;
        this.coordinators = coordinators;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Set<Stakeholder> getStakeholders() {
        return stakeholders;
    }

    public void setStakeholders(Set<Stakeholder> stakeholders) {
        this.stakeholders = stakeholders;
    }

    public Set<Coordinator> getCoordinators() {
        return coordinators;
    }

    public void setCoordinators(Set<Coordinator> coordinators) {
        this.coordinators = coordinators;
    }
}
