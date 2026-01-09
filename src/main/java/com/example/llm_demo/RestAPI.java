package com.example.llm_demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*  Setup:
    docker run -d --name pgvector -e POSTGRES_USER=app -e POSTGRES_PASSWORD=password -e POSTGRES_DB=demo -p 5432:5432 pgvector/pgvector:pg18-trixie
    docker run -d -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama
    docker exec -it ollama ollama pull llama3.1
 */

@RestController
@RequestMapping
public class RestAPI {
    private final Logger log = LoggerFactory.getLogger(RestAPI.class);
    private final ChatClient chatClient;

    @Autowired
    public RestAPI(VectorStore vectorStore, ChatClient.Builder chatClientBuilder, ToolCallbackProvider mcpTools) {
        this.chatClient = getChatClient(vectorStore, chatClientBuilder, mcpTools);
    }

    private ChatClient getChatClient(VectorStore vectorStore, ChatClient.Builder chatClientBuilder, ToolCallbackProvider mcpTools) {
        return chatClientBuilder
                .defaultAdvisors(getRagAdvisor(vectorStore), getLoggerAdvisor())
                .defaultToolCallbacks(mcpTools)
                //.defaultOptions(OllamaChatOptions.builder().temperature(0.3).topP(0.6).topK(1).repeatPenalty(1.1).build())
                .build();
    }

    private Advisor getRagAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                //.searchRequest(SearchRequest.builder().topK(1).similarityThreshold(0.8).build())
                .order(Ordered.HIGHEST_PRECEDENCE)
                .build();
    }

    private Advisor getLoggerAdvisor() {
        return SimpleLoggerAdvisor.builder().build();
    }

    @GetMapping("/query")
    public String query() {
        String userQuery = "Какой язык программирования высокоуровневый? И какая его последняя версия?";

        log.info("Processing...");

        String response = chatClient.prompt().user(userQuery).call().content();

        log.info("Processing finished");

        return response;
    }
}