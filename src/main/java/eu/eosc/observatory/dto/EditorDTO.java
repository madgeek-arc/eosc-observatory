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
package eu.eosc.observatory.dto;

import java.util.Date;

public class EditorDTO {

    private String email;
    private String fullname;
    private String role;
    private Date updateDate;

    public EditorDTO() {
    }

    public EditorDTO(String email, String role, Date updateDate) {
        this.email = email;
        this.role = role;
        this.updateDate = updateDate;
    }

    public EditorDTO(String email, String fullname, String role, Date updateDate) {
        this.email = email;
        this.fullname = fullname;
        this.role = role;
        this.updateDate = updateDate;
    }

    public String getEmail() {
        return email;
    }

    public EditorDTO setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getFullname() {
        return fullname;
    }

    public EditorDTO setFullname(String fullname) {
        this.fullname = fullname;
        return this;
    }

    public String getRole() {
        return role;
    }

    public EditorDTO setRole(String role) {
        this.role = role;
        return this;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public EditorDTO setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
        return this;
    }
}
