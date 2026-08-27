package screener.models;

import java.util.List;

public class JobProfile {
    private List<String> requiredSkills;
    private List<String> preferredSkills;

    public JobProfile(List<String> requiredSkills, List<String> preferredSkills) {
        this.requiredSkills = requiredSkills;
        this.preferredSkills = preferredSkills;
    }

    public List<String> getRequiredSkills() {
        return requiredSkills;
    }

    public List<String> getPreferredSkills() {
        return preferredSkills;
    }
}
