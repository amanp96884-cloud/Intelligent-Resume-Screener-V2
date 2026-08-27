package screener.engine;

import screener.models.CandidateResult;
import screener.models.JobProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ScoringEngine {

    private AISkillExtractor aiExtractor;

    public ScoringEngine() {
        // This automatically loads (or trains) your AI model when the engine initializes
        this.aiExtractor = new AISkillExtractor();
    }

    public CandidateResult evaluateCandidate(String fileName, String resumeText, JobProfile job) {

        // 1. Ask the AI to intelligently extract ALL skills from the text based on grammar
        List<String> aiFoundSkills = aiExtractor.extractSkillsFromText(resumeText);

        // 🚀 THE MAGIC REVEAL: Print exactly what the AI found autonomously
        System.out.println("🤖 AI autonomously discovered these skills for " + fileName + ": " + aiFoundSkills);

        // Convert found skills to lowercase for easier matching
        List<String> foundLower = aiFoundSkills.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        // 2. Cross-reference what the AI found against what the user required
        for (String req : job.getRequiredSkills()) {
            boolean matchFound = foundLower.stream().anyMatch(skill -> skill.contains(req.toLowerCase()));

            if (matchFound) {
                matchedSkills.add(req);
            } else {
                missingSkills.add(req);
            }
        }

        // 3. Calculate final score based on required skills
        double score = 0;
        if (!job.getRequiredSkills().isEmpty()) {
            score = ((double) matchedSkills.size() / job.getRequiredSkills().size()) * 100;
        }

        return new CandidateResult(fileName, matchedSkills, missingSkills, score);
    }
}