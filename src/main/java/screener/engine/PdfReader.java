package screener.engine;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;

public class PdfReader {

    // This method takes a file path, opens the PDF, and returns the text inside it
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
    // This method handles web uploads (InputStreams) instead of local files
    public String extractText(java.io.InputStream stream) {
        try (PDDocument document = PDDocument.load(stream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            System.err.println("Error reading uploaded PDF stream.");
            return "";
        }
    }
}
