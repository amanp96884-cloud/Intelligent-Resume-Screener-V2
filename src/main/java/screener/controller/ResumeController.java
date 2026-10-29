package screener.controller;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import screener.engine.PdfReader;
import screener.engine.ScoringEngine;
import screener.models.CandidateResult;
import screener.models.JobProfile;
import screener.repository.CandidateRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // fine for local development; restrict to your frontend domain in production
public class ResumeController {

    private final ScoringEngine scoringEngine;
    private final PdfReader pdfReader;
    private final CandidateRepository candidateRepository;

    // Constructor injection: Spring supplies all three beans
    public ResumeController(ScoringEngine scoringEngine,
                            PdfReader pdfReader,
                            CandidateRepository candidateRepository) {
        this.scoringEngine = scoringEngine;
        this.pdfReader = pdfReader;
        this.candidateRepository = candidateRepository;
    }

    /** Screen ONE resume. */
    @PostMapping("/screen")
    public CandidateResult screenResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "required", defaultValue = "") String requiredStr,
            @RequestParam(value = "preferred", defaultValue = "") String preferredStr) {

        if (file.isEmpty() || !isPdf(file)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please upload a non-empty PDF file");
        }

        JobProfile job = new JobProfile(parseSkills(requiredStr), parseSkills(preferredStr));

        try {
            return candidateRepository.save(scoreFile(file, job));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Failed to parse resume PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Screen MANY resumes in one request. Each resume is scored in parallel
     * (parallel stream over the files), then results are saved and returned ranked.
     */
    @PostMapping("/screen-batch")
    public List<CandidateResult> screenBatch(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "required", defaultValue = "") String requiredStr,
            @RequestParam(value = "preferred", defaultValue = "") String preferredStr) {

        if (files == null || files.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please upload at least one PDF");
        }

        JobProfile job = new JobProfile(parseSkills(requiredStr), parseSkills(preferredStr));

        // Parallel across resumes: ScoringEngine, PdfReader and AISkillExtractor keep no shared mutable state
        List<Outcome> outcomes = Arrays.stream(files)
                .parallel()
                .map(f -> scoreSafely(f, job))
                .toList();

        // Database writes stay sequential
        candidateRepository.saveAll(outcomes.stream()
                .filter(Outcome::ok)
                .map(Outcome::result)
                .toList());

        // Highest score first
        return outcomes.stream()
                .map(Outcome::result)
                .sorted(Comparator.comparingDouble(CandidateResult::getFinalScore).reversed())
                .toList();
    }

    /** All saved candidates, best score first. */
    @GetMapping("/ranking")
    public List<CandidateResult> ranking() {
        // Lazy @ElementCollection lists load fine here because Spring Boot's open-in-view is on by default
        return candidateRepository.findAll(Sort.by(Sort.Direction.DESC, "finalScore"));
    }

    // ---------- helpers ----------

    private record Outcome(CandidateResult result, boolean ok) {}

    private Outcome scoreSafely(MultipartFile file, JobProfile job) {
        String name = file.getOriginalFilename();
        if (file.isEmpty() || !isPdf(file)) {
            return new Outcome(new CandidateResult(name + " (failed: not a non-empty PDF)"), false);
        }
        try {
            return new Outcome(scoreFile(file, job), true);
        } catch (Exception e) {
            return new Outcome(new CandidateResult(name + " (failed: " + e.getMessage() + ")"), false);
        }
    }

    private CandidateResult scoreFile(MultipartFile file, JobProfile job) throws IOException {
        String text;
        try (InputStream in = file.getInputStream()) {
            text = pdfReader.extractText(in);
        }
        if (text == null || text.isBlank()) {
            throw new IOException("no readable text found (scanned PDF?)");
        }
        return scoringEngine.evaluateCandidate(file.getOriginalFilename(), text, job);
    }

    private static boolean isPdf(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name != null && name.toLowerCase().endsWith(".pdf");
    }

    private static List<String> parseSkills(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }
}