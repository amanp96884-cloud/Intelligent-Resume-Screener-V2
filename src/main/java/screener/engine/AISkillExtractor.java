package screener.engine;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.NameSampleDataStream;
import opennlp.tools.namefind.TokenNameFinderFactory;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.Span;
import opennlp.tools.util.TrainingParameters; // <-- This is the corrected one!

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AISkillExtractor {

    private TokenNameFinderModel model;

    public AISkillExtractor() {
        try {
            File modelFile = new File("skill-model.bin");

            // If the model hasn't been trained yet, train it!
            if (!modelFile.exists()) {
                trainModel(modelFile);
            }

            // Load the trained model into memory
            try (InputStream in = new FileInputStream(modelFile)) {
                model = new TokenNameFinderModel(in);
            }
        } catch (Exception e) {
            System.err.println("Failed to load AI model: " + e.getMessage());
        }
    }

    private void trainModel(File modelFile) throws IOException {
        System.out.println("🧠 Training AI Classification Model... Please wait.");

        // 1. Read the training data we just created
        InputStreamFactory in = new MarkableFileInputStreamFactory(new File("src/main/resources/skills.train"));
        ObjectStream<String> lineStream = new PlainTextByLineStream(in, StandardCharsets.UTF_8);
        ObjectStream<NameSample> sampleStream = new NameSampleDataStream(lineStream);

        // 2. Configure the Machine Learning Algorithm
        TrainingParameters params = new TrainingParameters();
        params.put(TrainingParameters.ITERATIONS_PARAM, 100);
        params.put(TrainingParameters.CUTOFF_PARAM, 1);

        // 3. Train the model
        TokenNameFinderFactory factory = new TokenNameFinderFactory();
        TokenNameFinderModel trainedModel = NameFinderME.train("en", "skill", sampleStream, params, factory);

        // 4. Save the trained brain to a .bin file
        try (OutputStream modelOut = new BufferedOutputStream(new FileOutputStream(modelFile))) {
            trainedModel.serialize(modelOut);
        }
        System.out.println("✅ AI Model successfully trained and saved as " + modelFile.getName());
    }

    public List<String> extractSkillsFromText(String resumeText) {
        // OpenNLP needs text broken down into individual words (tokens)
        String[] tokens = resumeText.split("\\s+");

        NameFinderME nameFinder = new NameFinderME(model);
        Span[] nameSpans = nameFinder.find(tokens);

        List<String> extractedSkills = new ArrayList<>();
        for (Span span : nameSpans) {
            StringBuilder skillBuilder = new StringBuilder();
            // Reconstruct multi-word skills (like "Spring Boot")
            for (int i = span.getStart(); i < span.getEnd(); i++) {
                skillBuilder.append(tokens[i]).append(" ");
            }
            extractedSkills.add(skillBuilder.toString().trim());
        }

        nameFinder.clearAdaptiveData();
        return extractedSkills;
    }
}
