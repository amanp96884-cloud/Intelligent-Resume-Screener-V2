package screener.models;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "candidate_results")
public class CandidateResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String candidateName;

    @ElementCollection
    private List<String> matchedSkills;

    @ElementCollection
    private List<String> missingSkills;

    private double finalScore;

    // Default constructor required by JPA
    public CandidateResult() {}

    public CandidateResult(String candidateName, List<String> matchedSkills, List<String> missingSkills, double finalScore) {
        this.candidateName = candidateName;
        this.matchedSkills = matchedSkills;
        this.missingSkills = missingSkills;
        this.finalScore = finalScore;
    }

    public CandidateResult(String errorMessage) {
        this.candidateName = errorMessage;
        this.matchedSkills = new ArrayList<>();
        this.missingSkills = new ArrayList<>();
        this.finalScore = 0.0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getCandidateName() { return candidateName; }
    public List<String> getMatchedSkills() { return matchedSkills; }
    public List<String> getMissingSkills() { return missingSkills; }
    public double getFinalScore() { return finalScore; }
}