package com.smarthire.config;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OCRConfig {

    @Value("${tesseract.data.path:/usr/share/tesseract-ocr/5/tessdata/}")
    private String tessDataPath;

    @Bean
    public Tesseract tesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("eng");
        return tesseract;
    }
}