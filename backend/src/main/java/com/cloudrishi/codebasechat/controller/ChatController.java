package com.cloudrishi.codebasechat.controller;

import com.cloudrishi.codebasechat.model.IndexRequest;
import com.cloudrishi.codebasechat.model.QueryRequest;
import com.cloudrishi.codebasechat.model.QueryResponse;
import com.cloudrishi.codebasechat.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing the codebase chat API endpoints.
 * <p>
 * Provides two core operations:
 * <ul>
 *   <li>Index — triggers ingestion of a Java repository into pgvector</li>
 *   <li>Chat — accepts a natural language question and returns an
 *       AI-generated answer grounded in the indexed codebase</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class ChatController {

    private final ChatService chatService;

    /**
     * Constructor injection of ChatService.
     *
     * @param chatService the core RAG orchestration service
     */
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Triggers indexing of a Java repository.
     * <p>
     * Should be called once before querying, and again
     * whenever the repository changes.
     * </p>
     *
     * @param request contains the absolute path to the Java repository
     * @return a status message confirming successful indexing
     */
    @PostMapping("/index")
    public ResponseEntity<String> index(@RequestBody IndexRequest request) {
        String status = chatService.index(request.getRepoPath());
        return ResponseEntity.ok(status);
    }

    /**
     * Accepts a natural language question about the indexed codebase
     * and returns an AI-generated answer with relevant source chunks.
     *
     * @param request contains the question and optional topK parameter
     * @return a {@link QueryResponse} with the answer and relevant chunks
     */
    @PostMapping("/chat")
    public ResponseEntity<QueryResponse> chat(@RequestBody QueryRequest request) {
        QueryResponse response = chatService.chat(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint.
     *
     * @return a simple ok status string
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("codebase-chat is running\n");
    }
}