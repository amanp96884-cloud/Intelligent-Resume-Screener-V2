package screener.engine;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Component
public class PdfReader {

    // Takes a file path, opens the PDF and returns the text inside it (unchanged from before)
    public String extractText(String filePath) {
        File file = new File(filePath);

        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);

        } catch (IOException e) {
            System.err.println("Error reading the PDF file: " + filePath);
            System.err.println(e.getMessage());
            return ""; // Return empty string if the file fails to read
        }
    }

    // Handles web uploads. Now throws, so the controller can report a real error
    // instead of silently scoring an unreadable PDF as 0.
    public String extractText(InputStream stream) throws IOException {
        try (PDDocument document = PDDocument.load(stream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}