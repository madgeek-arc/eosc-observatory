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

import com.fasterxml.jackson.annotation.JsonIgnore;
import eu.openaire.observatory.service.Identifiable;

import java.util.*;
import java.util.stream.Collectors;

public class UserGroup implements Identifiable<String> {

    protected String id;
    protected String name;
    protected String type;
    protected SortedSet<String> admins;
    protected SortedSet<String> members;

    public UserGroup() {

    }

    @Override
    public String getId() {
        return id;
    }

    @Override
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

    public SortedSet<String> getAdmins() {
        return Optional.ofNullable(this.admins).orElse(new TreeSet<>());
    }

    public void setAdmins(Set<String> admins) {
        this.admins = admins == null ? null : admins
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public SortedSet<String> getMembers() {
        return Optional.ofNullable(this.members).orElse(new TreeSet<>());
    }

    public void setMembers(Set<String> members) {
        this.members = members == null ? null : members
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @JsonIgnore
    public final Set<String> getUsers() {
        Set<String> users = new TreeSet<>();
        users.addAll(Optional.ofNullable(this.admins).orElse(new TreeSet<>()));
        users.addAll(Optional.ofNullable(this.members).orElse(new TreeSet<>()));
        return users;
    }

    public enum GroupType {
        COUNTRY("country"),
        EOSC_SB("eosc-sb"),
        EOSC_ASSOCIATION("eosc-a"),
        CLIMATE("climate"),
        AI("ai");

        private final String type;

        GroupType(String type) {
            this.type = type;
        }

        public String getKey() {
            return type;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static GroupType fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(GroupType.values())
                    .filter(v -> v.type.equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }
    }
}
