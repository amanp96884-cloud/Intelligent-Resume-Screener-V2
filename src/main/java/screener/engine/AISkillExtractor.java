package screener.engine;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.NameSampleDataStream;
import opennlp.tools.namefind.TokenNameFinderFactory;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.Span;
import opennlp.tools.util.TrainingParameters;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class AISkillExtractor {

    private static final File MODEL_FILE = new File("skill-model.bin");
    private static final String TRAIN_RESOURCE = "/skills.train"; // read from the classpath, so it also works inside a jar

    // The loaded model is immutable and safe to share across threads.
    private final TokenNameFinderModel model;

    public AISkillExtractor() {
        TokenNameFinderModel loaded;
        try {
            if (!MODEL_FILE.exists()) {
                trainModel();
            }
            try (InputStream in = new FileInputStream(MODEL_FILE)) {
                loaded = new TokenNameFinderModel(in);
            }
        } catch (IOException e) {
            // Fail fast instead of continuing with a null model
            throw new IllegalStateException("Could not load or train the skill model", e);
        }
        this.model = loaded;
    }

    private void trainModel() throws IOException {
        InputStreamFactory factory = () -> {
            InputStream in = AISkillExtractor.class.getResourceAsStream(TRAIN_RESOURCE);
            if (in == null) {
                throw new IOException("Training file not found on classpath: " + TRAIN_RESOURCE);
            }
            return in;
        };

        try (ObjectStream<String> lines = new PlainTextByLineStream(factory, StandardCharsets.UTF_8);
             ObjectStream<NameSample> samples = new NameSampleDataStream(lines)) {

            TrainingParameters params = new TrainingParameters();
            params.put(TrainingParameters.ITERATIONS_PARAM, 100);
            params.put(TrainingParameters.CUTOFF_PARAM, 1);

            TokenNameFinderModel trained = NameFinderME.train(
                    "en", "skill", samples, params, new TokenNameFinderFactory());

            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(MODEL_FILE))) {
                trained.serialize(out);
            }
        }
    }

    public List<String> extractSkillsFromText(String resumeText) {
        String[] tokens = tokenize(resumeText);
        if (tokens.length == 0) {
            return List.of();
        }

        // NameFinderME is NOT thread-safe, so create a new one per call (the model itself is shared)
        NameFinderME nameFinder = new NameFinderME(model);
        Span[] spans = nameFinder.find(tokens);

        Set<String> skills = new LinkedHashSet<>(); // removes duplicates, keeps order
        for (Span span : spans) {
            skills.add(String.join(" ", Arrays.copyOfRange(tokens, span.getStart(), span.getEnd())));
        }
        return new ArrayList<>(skills);
    }

    /**
     * Must match how skills.train is written: punctuation like , ; : ( ) | is its own token,
     * a trailing full stop is split off, but c++, c#, node.js and CI/CD stay as single tokens.
     */
    static String[] tokenize(String text) {
        if (text == null || text.isBlank()) {
            return new String[0];
        }
        String spaced = text
                .replaceAll("([,;:()\\[\\]|\\u2022\\u25CF\\u25AA])", " $1 ")
                .replaceAll("(\\S)\\.(?=\\s|$)", "$1 .");
        return spaced.trim().split("\\s+");
    }
}