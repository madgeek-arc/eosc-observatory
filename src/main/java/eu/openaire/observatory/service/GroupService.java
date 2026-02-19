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

package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.Stakeholder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class GroupService {

    private final CoordinatorService coordinatorService;
    private final StakeholderService stakeholderService;


    public GroupService(CoordinatorService coordinatorService,
                        StakeholderService stakeholderService) {
        this.coordinatorService = coordinatorService;
        this.stakeholderService = stakeholderService;
    }

    public String getGroupName(String groupId) {
        String groupName = "";
        if (groupId.startsWith("sh-")) {
            groupName = stakeholderService.get(groupId).getName();
        } else if (groupId.startsWith("co-")) {
            groupName = coordinatorService.get(groupId).getName();
        }
        return groupName;
    }

    public Set<String> getUserIds(String groupId) {
        Set<String> members = new HashSet<>();
        if (groupId.startsWith("sh-")) {
            Stakeholder sh = stakeholderService.get(groupId);
            members.addAll(sh.getAdmins());
            members.addAll(sh.getMembers());
        } else if (groupId.startsWith("co-")) {
            members.addAll(coordinatorService.get(groupId).getMembers());
        }
        return members;
    }
}
