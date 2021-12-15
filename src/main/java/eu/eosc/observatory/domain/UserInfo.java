package eu.eosc.observatory.domain;

import java.util.Set;

public class UserInfo {
    private User user;
    private Set<Stakeholder> memberOf;

    public UserInfo() {}

    public UserInfo(User user, Set<Stakeholder> memberOf) {
        this.user = user;
        this.memberOf = memberOf;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Set<Stakeholder> getMemberOf() {
        return memberOf;
    }

    public void setMemberOf(Set<Stakeholder> memberOf) {
        this.memberOf = memberOf;
    }
}
