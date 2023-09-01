package eu.eosc.observatory.dto;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GroupMembers<T> {

    Set<T> members;
    Set<T> admins;

    public GroupMembers() {
    }

    public GroupMembers(Set<T> members, Set<T> admins) {
        this.members = members;
        this.admins = admins;
    }

    public <U> GroupMembers<U> map(Function<? super T, ? extends U> converter) {
        return new GroupMembers<>(
                this.getMembers().stream().map(converter).collect(Collectors.toSet()),
                this.getAdmins().stream().map(converter).collect(Collectors.toSet())
        );
    }

    public Set<T> getMembers() {
        return members;
    }

    public void setMembers(Set<T> members) {
        this.members = members;
    }

    public Set<T> getAdmins() {
        return admins;
    }

    public void setAdmins(Set<T> admins) {
        this.admins = admins;
    }
}
