package screener.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import screener.engine.PdfReader;
import screener.engine.ScoringEngine;
import screener.models.CandidateResult;
import screener.models.JobProfile;
import screener.repository.CandidateRepository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ResumeController {

    @Autowired
    private CandidateRepository candidateRepository; // Connects your database

    @PostMapping("/screen")
    public CandidateResult screenResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "required", defaultValue = "") String required,
            @RequestParam(value = "preferred", defaultValue = "") String preferred) {

        System.out.println("---- NEW REQUEST ----");
        System.out.println("1. API Endpoint Reached! File received: " + file.getOriginalFilename());

        try {
            PdfReader reader = new PdfReader();

            System.out.println("2. Initializing AI Engine...");
            ScoringEngine engine = new ScoringEngine();

            System.out.println("3. AI Engine ready! Extracting text...");

            List<String> requiredSkills = Arrays.stream(required.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            List<String> preferredSkills = Arrays.stream(preferred.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            JobProfile job = new JobProfile(requiredSkills, preferredSkills);

            String text = reader.extractText(file.getInputStream());

            System.out.println("4. Text extracted! Handing over to AI for evaluation...");
            CandidateResult result = engine.evaluateCandidate(file.getOriginalFilename(), text, job);

            // 💾 PERSIST TO THE H2 DATABASE
            candidateRepository.save(result);
            System.out.println("💾 Saved " + file.getOriginalFilename() + " permanently to the database!");

            System.out.println("5. Evaluation complete! Sending JSON back to browser.");
            return result;

        } catch (Exception e) {
            System.err.println("API Error: " + e.getMessage());
            e.printStackTrace();
            return new CandidateResult("Error Processing Upload");
        }
    }
}