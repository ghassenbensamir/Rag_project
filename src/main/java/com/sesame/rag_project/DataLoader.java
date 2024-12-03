package com.sesame.rag_project;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class DataLoader {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    private final VectorStore vectorStore;
    private final JdbcClient jdbcClient;

    @Value("classpath:/Exemple_DS_Corr.pdf")
    private Resource pdfResource;

    public DataLoader(VectorStore vectorStore, JdbcClient jdbcClient) {
        this.vectorStore = vectorStore;
        this.jdbcClient = jdbcClient;
    }

    @PostConstruct
    public void init() {
        try {
            int count = getRecordCount();
            logger.info("Records count: {}", count);

            if (count == 0) {
                processAndStorePdf();
            } else {
                logger.info("Data already exists. Skipping PDF processing.");
            }
        } catch (Exception e) {
            logger.error("Error during data loading process: ", e);
        }
    }

    private int getRecordCount() {
        try {
            return jdbcClient.sql("SELECT COUNT(*) FROM vector_store")
                    .query(Integer.class)
                    .single();
        } catch (Exception e) {
            logger.error("Error querying record count: ", e);
            throw new RuntimeException("Failed to fetch record count from the database.", e);
        }
    }

    private void processAndStorePdf() {
        try {
            logger.info("Starting PDF processing...");
            PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                    .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder()
                            .withNumberOfBottomTextLinesToDelete(0)
                            .withNumberOfTopPagesToSkipBeforeDelete(0)
                            .build())
                    .withPagesPerDocument(1)
                    .build();

            PagePdfDocumentReader reader = new PagePdfDocumentReader(pdfResource, config);
            TokenTextSplitter textSplitter = new TokenTextSplitter();

            vectorStore.accept(textSplitter.apply(reader.get()));
            logger.info("PDF processing and data storage completed successfully.");
        } catch (Exception e) {
            logger.error("Error processing the PDF document: ", e);
            throw new RuntimeException("Failed to process and store PDF data.", e);
        }
    }
}
