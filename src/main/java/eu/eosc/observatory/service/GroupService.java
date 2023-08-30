package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Stakeholder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
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

    public List<String> getUserIds(String groupId) {
        Set<String> members = new HashSet<>();
        if (groupId.startsWith("sh-")) {
            Stakeholder sh = stakeholderService.get(groupId);
            members.addAll(sh.getManagers());
            members.addAll(sh.getContributors());
        } else if (groupId.startsWith("co-")) {
            members.addAll(coordinatorService.get(groupId).getMembers());
        }
        return members.stream().toList();
    }
}
