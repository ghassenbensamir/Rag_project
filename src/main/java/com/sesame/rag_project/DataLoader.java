package com.sesame.rag_project;


import jakarta.annotation.PostConstruct;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;


@Component
public class DataLoader {
    private final VectorStore vectorStore;
    private final JdbcClient jdbcClient;

    @Value("classpath:/Exemple_DS_Corr.pdf")
    private Resource pdfResource;
    public DataLoader(VectorStore vectorStore, JdbcClient jdbcClient, ResourceLoader resourceLoader) {
        this.vectorStore = vectorStore;
        this.jdbcClient = jdbcClient;
    }


    @PostConstruct
    public void init() {
        Integer count=jdbcClient.sql("select COUNT(*) from vector_store").
                query(Integer.class).
                single();
        System.out.println("Records count:"+count);

        //if we don't have any data
        if (count == 0) {
            //configuration to read the pdf
            PdfDocumentReaderConfig config= PdfDocumentReaderConfig.builder().
                    withPagesPerDocument(1)
                    .build();
            //read the pdf
            PagePdfDocumentReader reader=new PagePdfDocumentReader(pdfResource,config);
            //split pdf to documents
            var textsplitter=new TokenTextSplitter();
            //store docs to vectorStore
            vectorStore.accept(textsplitter.apply(reader.get()));

        }
    }


}
