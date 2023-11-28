package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Stakeholder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
