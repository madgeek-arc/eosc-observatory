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

import eu.eosc.observatory.domain.Stakeholder;

public class StakeholderInfo {

    String id;
    String name;
    String type;
    String subType;
    String country;
    String associationMember;

    public StakeholderInfo() {
        // no-arg constructor
    }

    public static StakeholderInfo of(Stakeholder stakeholder) {
        StakeholderInfo info = new StakeholderInfo();
        info.setId(stakeholder.getId());
        info.setName(stakeholder.getName());
        info.setType(stakeholder.getType());
        info.setSubType(stakeholder.getSubType());
        info.setCountry(stakeholder.getCountry());
        info.setAssociationMember(stakeholder.getAssociationMember());
        return info;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
}
