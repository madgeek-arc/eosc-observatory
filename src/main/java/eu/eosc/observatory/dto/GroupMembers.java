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

import java.util.Objects;
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
        return new GroupMembers<>(this.getMembers() == null ? null :
                this.getMembers().stream().filter(Objects::nonNull).map(converter).collect(Collectors.toSet()),
                this.getAdmins() == null ? null :
                        this.getAdmins().stream().filter(Objects::nonNull).map(converter).collect(Collectors.toSet())
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
