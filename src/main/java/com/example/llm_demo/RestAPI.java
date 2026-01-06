package com.example.llm_demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class RestAPI {
    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final ToolCallbackProvider mcpTools;

    @Autowired
    public RestAPI(VectorStore vectorStore, ChatClient.Builder chatClientBuilder, ToolCallbackProvider mcpTools) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
        this.mcpTools = mcpTools;
    }

    @GetMapping("/query")
    public String query() {
        String userQuery = "Какой язык программирования высокоуровневый? И какая его последняя версия?";

        System.out.println("Processing...");

        List<Document> results = vectorStore
                .similaritySearch(SearchRequest.builder().query(userQuery).topK(3).build());

        StringBuilder context = new StringBuilder();
        results.stream()
                .map(Document::getText)
                .forEach(context::append);

        PromptTemplate template = new PromptTemplate("Ты программист-консультант. Ответ не должен превышать 100 символов  " +
                "CONTEXT: \n" +
                " {context} \n" +
                "QUESTION: \n" +
                " {question}");
        Prompt prompt = template.create(Map.of(
                "context", context.toString(),
                "question", userQuery
        ));

        String response = chatClient.prompt(prompt)
                .toolCallbacks(mcpTools)
                .call()
                .content();

        System.out.println("response = " + response);

        return response;
    }
}