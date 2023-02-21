package eu.eosc.observatory.domain;

import java.util.Set;

public class UserInfo {
    private User user;
    private boolean isAdmin;
    private Set<Stakeholder> stakeholders;
    private Set<Coordinator> coordinators;

    public UserInfo() {
        // no-arg constructor
    }

    public UserInfo(User user, boolean isAdmin, Set<Stakeholder> stakeholders, Set<Coordinator> coordinators) {
        this.user = user;
        this.isAdmin = isAdmin;
        this.stakeholders = stakeholders;
        this.coordinators = coordinators;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
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
