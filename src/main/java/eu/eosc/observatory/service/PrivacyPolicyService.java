package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.PrivacyPolicy;

public interface PrivacyPolicyService extends CrudService<PrivacyPolicy> {

    PrivacyPolicy getLatestByType(String type);

    boolean hasAcceptedPolicy(String policyDoi, String userId);
}
