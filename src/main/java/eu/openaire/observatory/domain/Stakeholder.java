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
package eu.openaire.observatory.domain;

import java.util.Objects;

public class Stakeholder extends UserGroup {

    String subType;
    String country; // 2-letter code
    String associationMember; // will create list at some point
    boolean mandated;

    public Stakeholder() {
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getAssociationMember() {
        return associationMember;
    }

    public void setAssociationMember(String associationMember) {
        this.associationMember = associationMember;
    }

    public boolean isMandated() {
        return mandated;
    }

    public void setMandated(boolean mandated) {
        this.mandated = mandated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stakeholder)) return false;
        Stakeholder that = (Stakeholder) o;
        return id.equals(that.id) && Objects.equals(name, that.name) && Objects.equals(type, that.type) && Objects.equals(subType, that.subType) && Objects.equals(country, that.country) && Objects.equals(associationMember, that.associationMember) && Objects.equals(admins, that.admins) && Objects.equals(members, that.members);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, type, subType, country, associationMember, admins, members);
    }
}
