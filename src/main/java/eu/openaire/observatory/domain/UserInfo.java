/*
 * Copyright 2021-2026 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.openaire.observatory.domain;

import java.util.Set;

public class UserInfo {
    private User user;
    private boolean isAdmin;
    private Set<Stakeholder> stakeholders;
    private Set<Coordinator> coordinators;
    private Set<Administrator> administrators;

    public UserInfo() {
        // no-arg constructor
    }

    public UserInfo(User user, boolean isAdmin, Set<Stakeholder> stakeholders, Set<Coordinator> coordinators, Set<Administrator> administrators) {
        this.user = user;
        this.isAdmin = isAdmin;
        this.stakeholders = stakeholders;
        this.coordinators = coordinators;
        this.administrators = administrators;
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

    public Set<Administrator> getAdministrators() {
        return administrators;
    }

    public UserInfo setAdministrators(Set<Administrator> administrators) {
        this.administrators = administrators;
        return this;
    }
}
