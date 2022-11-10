package eu.eosc.observatory.dto;

import java.util.Set;

public class ResourcePermissions {

    String resourceId;
    Set<String> permissions;

    public ResourcePermissions() {
        // no-arg constructor
    }

    public ResourcePermissions(String resourceId, Set<String> permissions) {
        this.resourceId = resourceId;
        this.permissions = permissions;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
}
