/**
 * Copyright 2021-2025 OpenAIRE AMKE
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
package eu.eosc.observatory.domain;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class Editor implements Serializable {
    private String user;
    private String role;
    private Date updateDate = new Date();

    public Editor() {
        // no-arg constructor
    }

    public Editor(String user, String role) {
        this.user = user;
        this.role = role;
    }

    public String getUser() {
        return user;
    }

    public Editor setUser(String user) {
        this.user = user;
        return this;
    }

    public String getRole() {
        return role;
    }

    public Editor setRole(String role) {
        this.role = role;
        return this;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public Editor setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
        return this;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Editor editor = (Editor) object;
        return Objects.equals(user, editor.user) && Objects.equals(role, editor.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, role);
    }
}
