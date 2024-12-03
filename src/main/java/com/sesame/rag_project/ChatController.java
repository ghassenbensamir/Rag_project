package com.sesame.rag_project;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController

public class ChatController {

    private final ChatModel model;
    private final VectorStore vectorStore;
    private final ChatModel chatModel;

    private String prompt= """
    You are a helpful and friendly AI assistant who can answer questions. Use the information from the DOCUMENTS
    section to provide accurate answers..
    Your task is to answer questions based on the informations from the documents section to provide accurate answer.
  if unsure or information isn't found simply state that you don't know the answer
  QUESTION:
  {question}
  
  DOCUMENTS:
  {documents}
  """;
    public ChatController(ChatModel model, VectorStore vectorStore, ChatModel chatModel) {
        this.model = model;
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
    }

    @GetMapping("/")
    public  String generateAnswer(@RequestParam(value = "question",defaultValue = "List all question in document") String question ) {
        PromptTemplate promptTemplate = new PromptTemplate(prompt);
        Map<String,Object> promptParams = new HashMap<>();
        promptParams.put("question",question);
        promptParams.put("documents", findSimilarData(question));

        return  chatModel.call(promptTemplate.create(promptParams)).
                getResult().
                getOutput().
                getContent();
    }

    private String findSimilarData(String question) {
        List<Document> similarDocuments = vectorStore.
                similaritySearch(SearchRequest.
                        query(question).
                        withTopK(3));
        return  similarDocuments.stream().map(Document::toString).collect(Collectors.joining());
    }

}
