package com.smarthire.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

@Service
public class ResumeTextExtractor {

    @Value("${tesseract.data.path:/opt/homebrew/share/tessdata/}")
    private String tessDataPath;

    public String extractText(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();

        if (fileName == null) {
            throw new IOException("Invalid file name");
        }

        System.out.println("Extracting from: " + fileName);

        if (fileName.endsWith(".pdf")) {
            return extractFromPdf(file);
        } else if (fileName.endsWith(".docx")) {
            return extractFromDocx(file);
        } else if (fileName.endsWith(".txt")) {
            return extractFromTxt(file);
        } else {
            throw new IOException("Unsupported file format. Please upload PDF, DOCX, or TXT files.");
        }
    }

    private String extractFromPdf(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {

            // First try: Normal text extraction
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            System.out.println("Normal PDF text extraction length: " + (text != null ? text.length() : 0));

            // If text is empty or very short, PDF might be scanned
            if (text == null || text.trim().length() < 50) {
                System.out.println("PDF appears to be scanned. Using OCR...");

                // Use OCR for scanned PDFs
                String ocrText = extractTextWithOCR(document);
                if (ocrText != null && !ocrText.trim().isEmpty()) {
                    System.out.println("OCR extraction successful. Length: " + ocrText.length());
                    return ocrText;
                }

                System.err.println("OCR extraction failed. Returning empty text.");
                return "";
            }

            return text;
        } catch (Exception e) {
            System.err.println("PDF extraction error: " + e.getMessage());
            return "";
        }
    }

    private String extractTextWithOCR(PDDocument document) {
        try {
            // Initialize Tesseract
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage("eng");

            System.out.println("Tesseract data path: " + tessDataPath);

            // Render PDF pages to images and run OCR
            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder fullText = new StringBuilder();

            int pageCount = document.getNumberOfPages();
            System.out.println("Processing " + pageCount + " pages with OCR...");

            for (int page = 0; page < pageCount; page++) {
                // Render page as image (300 DPI for better accuracy)
                BufferedImage image = renderer.renderImageWithDPI(page, 300);

                // Run OCR on the image
                String pageText = tesseract.doOCR(image);
                fullText.append(pageText).append("\n");

                System.out.println("Page " + (page + 1) + " OCR complete. Text length: " + pageText.length());
            }

            return fullText.toString();

        } catch (TesseractException e) {
            System.err.println("Tesseract OCR error: " + e.getMessage());
            return "";
        } catch (Exception e) {
            System.err.println("OCR processing error: " + e.getMessage());
            return "";
        }
    }

    private String extractFromDocx(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {

            String text = extractor.getText();
            System.out.println("DOCX text length: " + (text != null ? text.length() : 0));
            return text != null ? text : "";
        } catch (Exception e) {
            System.err.println("DOCX extraction error: " + e.getMessage());
            return "";
        }
    }

    private String extractFromTxt(MultipartFile file) throws IOException {
        String text = new String(file.getBytes());
        System.out.println("TXT text length: " + text.length());
        return text;
    }
}