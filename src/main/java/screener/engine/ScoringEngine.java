package screener.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import screener.models.CandidateResult;
import screener.models.JobProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ScoringEngine {

    private static final Logger log = LoggerFactory.getLogger(ScoringEngine.class);

    private static final double REQUIRED_WEIGHT = 0.80;
    private static final double PREFERRED_WEIGHT = 0.20;

    // If true, a skill also counts when it appears as a whole word in the resume text,
    // even if the NER model missed it. This guarantees recall; set to false to score on NER only.
    private static final boolean KEYWORD_FALLBACK = true;

    private final AISkillExtractor aiExtractor;

    public ScoringEngine(AISkillExtractor aiExtractor) {
        this.aiExtractor = aiExtractor;
    }

    public CandidateResult evaluateCandidate(String fileName, String resumeText, JobProfile job) {

        // 1. Skills found by the trained NER model, normalized for exact comparison
        Set<String> nerSkills = aiExtractor.extractSkillsFromText(resumeText).stream()
                .map(ScoringEngine::normalize)
                .collect(Collectors.toSet());
        log.info("NER found {} skills in {}: {}", nerSkills.size(), fileName, nerSkills);

        String textLower = resumeText == null ? "" : resumeText.toLowerCase();

        List<String> required = job.getRequiredSkills() != null ? job.getRequiredSkills() : List.of();
        List<String> preferred = job.getPreferredSkills() != null ? job.getPreferredSkills() : List.of();

        // 2. Required skills: matched / missing
        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();
        for (String req : required) {
            if (hasSkill(req, nerSkills, textLower)) {
                matchedSkills.add(req);
            } else {
                missingSkills.add(req);
            }
        }
        double requiredScore = required.isEmpty()
                ? 0 : ((double) matchedSkills.size() / required.size()) * 100.0;

        // 3. Preferred skills
        long matchedPreferred = preferred.stream()
                .filter(p -> hasSkill(p, nerSkills, textLower))
                .count();
        double preferredScore = preferred.isEmpty()
                ? 0 : ((double) matchedPreferred / preferred.size()) * 100.0;

        // 4. Weighted score: 80% required + 20% preferred (falls back if one list is empty)
        double finalScore;
        if (required.isEmpty() && preferred.isEmpty()) {
            finalScore = 0;
        } else if (required.isEmpty()) {
            finalScore = preferredScore;
        } else if (preferred.isEmpty()) {
            finalScore = requiredScore;
        } else {
            finalScore = (requiredScore * REQUIRED_WEIGHT) + (preferredScore * PREFERRED_WEIGHT);
        }
        finalScore = Math.round(finalScore * 100.0) / 100.0;

        return new CandidateResult(fileName, matchedSkills, missingSkills, finalScore);
    }

    /** Exact match on normalized skill names ("java" does NOT match "javascript"). */
    private static boolean hasSkill(String skill, Set<String> nerSkills, String textLower) {
        String key = normalize(skill);
        if (key.isEmpty()) {
            return false;
        }
        if (nerSkills.contains(key)) {
            return true;
        }
        if (!KEYWORD_FALLBACK) {
            return false;
        }
        // Whole-word search: not preceded/followed by a letter, digit, '+' or '#'
        Pattern p = Pattern.compile("(?<![a-z0-9+#])" + Pattern.quote(key) + "(?![a-z0-9+#])");
        return p.matcher(textLower).find();
    }

    /** Lowercase, trim, strip stray punctuation at the edges but keep c++, c#, node.js intact. */
    static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase().trim()
                .replaceAll("^[^a-z0-9.]+", "")
                .replaceAll("[^a-z0-9+#]+$", "");
    }
}