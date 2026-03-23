package com.smarthire.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
public class ResumeTextExtractor {

    public String extractText(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();

        if (fileName == null) {
            throw new IOException("Invalid file name");
        }


        if (fileName.endsWith(".pdf") || "application/pdf".equals(contentType)) {
            return extractFromPdf(file);
        } else if (fileName.endsWith(".docx") ||
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)) {
            return extractFromDocx(file);
        } else if (fileName.endsWith(".txt") || "text/plain".equals(contentType)) {
            return extractFromTxt(file);
        } else {
            throw new IOException("Unsupported file format. Please upload PDF, DOCX, or TXT files.");
        }
    }

    private String extractFromPdf(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text == null || text.trim().isEmpty()) {
                throw new IOException("Could not extract text from PDF. The file might be scanned or image-based.");
            }

            return text;
        } catch (Exception e) {
            throw new IOException("Error extracting text from PDF: " + e.getMessage(), e);
        }
    }

    private String extractFromDocx(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {

            String text = extractor.getText();

            if (text == null || text.trim().isEmpty()) {
                throw new IOException("Could not extract text from DOCX file.");
            }

            return text;
        } catch (Exception e) {
            throw new IOException("Error extracting text from DOCX: " + e.getMessage(), e);
        }
    }

    private String extractFromTxt(MultipartFile file) throws IOException {
        return new String(file.getBytes());
    }
}