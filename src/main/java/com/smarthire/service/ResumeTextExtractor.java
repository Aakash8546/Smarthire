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

    @Value("${tesseract.data.path:/usr/share/tesseract-ocr/5/tessdata/}")
    private String tessDataPath;

    private Boolean tesseractAvailable = null;

    public String extractText(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();

        if (fileName == null) {
            throw new IOException("Invalid file name");
        }

        System.out.println("========================================");
        System.out.println("Extracting from: " + fileName);
        System.out.println("File size: " + file.getSize() + " bytes");
        System.out.println("========================================");

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

            System.out.println("📄 Step 1: Trying PDFBox text extraction...");

            // Step 1: Try PDFBox normal text extraction
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            int textLength = text != null ? text.trim().length() : 0;
            System.out.println("   PDFBox extracted " + textLength + " characters");

            // Step 1 Result: If we got enough text, return it
            if (textLength > 50) {
                System.out.println("✅ PDFBox extraction successful! Returning text.");
                return text;
            }

            // Step 1 Result: Not enough text, PDF might be scanned
            System.out.println("⚠️ PDFBox extraction failed or insufficient text.");
            System.out.println("   This PDF appears to be scanned or image-based.");

            // Step 2: Check OCR availability
            System.out.println("\n🔍 Step 2: Checking OCR availability...");

            if (isTesseractAvailable()) {
                System.out.println("✅ OCR is available. Using Tesseract for scanned PDF...");
                return extractTextWithOCR(document);
            } else {
                // Step 2 Result: OCR not available - show friendly fallback message
                System.out.println("❌ OCR is NOT available on this server.");
                System.out.println("   Returning friendly error message.");
                throw new IOException(generateFriendlyFallbackMessage());
            }

        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("SCANNED_PDF")) {
                throw e;
            }
            throw new IOException("Error extracting text from PDF: " + e.getMessage(), e);
        }
    }

    private String extractTextWithOCR(PDDocument document) {
        try {
            System.out.println("\n📸 Step 3: Running OCR on scanned PDF...");

            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage("eng");

            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder fullText = new StringBuilder();

            int pageCount = document.getNumberOfPages();
            System.out.println("   Total pages: " + pageCount);

            for (int page = 0; page < pageCount; page++) {
                System.out.print("   Processing page " + (page + 1) + "... ");
                BufferedImage image = renderer.renderImageWithDPI(page, 300);
                String pageText = tesseract.doOCR(image);
                fullText.append(pageText).append("\n");
                System.out.println("Extracted " + pageText.length() + " chars");
            }

            System.out.println("✅ OCR extraction complete! Total text length: " + fullText.length());
            return fullText.toString();

        } catch (TesseractException e) {
            System.err.println("❌ Tesseract OCR error: " + e.getMessage());
            return "";
        } catch (Exception e) {
            System.err.println("❌ OCR processing error: " + e.getMessage());
            return "";
        }
    }

    private boolean isTesseractAvailable() {
        if (tesseractAvailable != null) {
            return tesseractAvailable;
        }

        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage("eng");

            // Simple test to check if tesseract works
            BufferedImage testImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            tesseract.doOCR(testImage);

            tesseractAvailable = true;
            System.out.println("   ✓ Tesseract OCR is available");
            return true;

        } catch (Exception e) {
            tesseractAvailable = false;
            System.out.println("   ✗ Tesseract OCR is NOT available");
            System.out.println("     Reason: " + e.getMessage());
            return false;
        }
    }

    private String generateFriendlyFallbackMessage() {
        return """
            ╔══════════════════════════════════════════════════════════════╗
            ║           📄 SCANNED PDF NOT SUPPORTED                        ║
            ╚══════════════════════════════════════════════════════════════╝
            
            We detected that you uploaded a scanned or image-based PDF file.
            Our server currently does not support scanned PDFs.
            
            🔧 **How to fix this (3 easy ways):**
            
            ┌─────────────────────────────────────────────────────────────┐
            │ Method 1: Convert using Google Docs (Free & Easy)          │
            ├─────────────────────────────────────────────────────────────┤
            │ 1. Go to drive.google.com                                  │
            │ 2. Upload your scanned PDF                                 │
            │ 3. Right-click → Open with → Google Docs                   │
            │ 4. File → Download → PDF Document (.pdf)                   │
            │ 5. Upload the new PDF file                                 │
            └─────────────────────────────────────────────────────────────┘
            
            ┌─────────────────────────────────────────────────────────────┐
            │ Method 2: Create a text-based PDF (Recommended)            │
            ├─────────────────────────────────────────────────────────────┤
            │ 1. Open Microsoft Word or Google Docs                      │
            │ 2. Type/Copy your resume content                          │
            │ 3. File → Save As → PDF                                    │
            │ 4. Upload the text-based PDF                               │
            └─────────────────────────────────────────────────────────────┘
            
            ┌─────────────────────────────────────────────────────────────┐
            │ Method 3: Use DOCX or TXT format                          │
            ├─────────────────────────────────────────────────────────────┤
            │ • DOCX (Word document) - Fully supported                   │
            │ • TXT (Plain text) - Fully supported                       │
            │ • Upload your resume in either format                      │
            └─────────────────────────────────────────────────────────────┘
            
            💡 **Need more help?**
            • Email: support@smarthire.com
            • Help Center: https://smarthire.com/help
            
            We're here to help! ✨
            """;
    }

    private String extractFromDocx(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {

            String text = extractor.getText();
            int textLength = text != null ? text.length() : 0;
            System.out.println("DOCX text extraction complete. Length: " + textLength);

            if (text == null || text.trim().isEmpty()) {
                throw new IOException("Could not extract text from DOCX file. File might be corrupted or empty.");
            }

            return text;
        } catch (Exception e) {
            throw new IOException("Error extracting text from DOCX: " + e.getMessage(), e);
        }
    }

    private String extractFromTxt(MultipartFile file) throws IOException {
        String text = new String(file.getBytes());
        System.out.println("TXT text extraction complete. Length: " + text.length());

        if (text.trim().isEmpty()) {
            throw new IOException("TXT file is empty. Please upload a file with content.");
        }

        return text;
    }
}