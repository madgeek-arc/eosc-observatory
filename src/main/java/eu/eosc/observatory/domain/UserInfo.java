package eu.eosc.observatory.domain;

import java.util.List;

public class UserInfo {
    private User user;
    private List<Stakeholder> memberOf;

    public UserInfo() {}

    public UserInfo(User user, List<Stakeholder> memberOf) {
        this.user = user;
        this.memberOf = memberOf;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Stakeholder> getMemberOf() {
        return memberOf;
    }

    public void setMemberOf(List<Stakeholder> memberOf) {
        this.memberOf = memberOf;
    }
}
