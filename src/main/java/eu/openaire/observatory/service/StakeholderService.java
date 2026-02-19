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
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.dto.UserDTO;

import java.util.List;
import java.util.Set;

public interface StakeholderService extends CrudService<Stakeholder>, UserGroupService {

    Stakeholder updateStakeholderAndUserPermissions(String stakeholderId, Stakeholder stakeholder);

    Stakeholder updateContributors(String stakeholderId, Set<String> emails);

    Stakeholder updateManagers(String stakeholderId, Set<String> emails);

    List<UserDTO> getManagers(String stakeholderId);

}
