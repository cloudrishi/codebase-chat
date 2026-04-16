package com.cloudrishi.codebasechat.client;

import com.cloudrishi.codebasechat.model.CodeChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * HTTP client responsible for communicating with the Python AI Worker microservice.
 * <p>
 * The AI Worker handles all ML operations including Java AST parsing,
 * embedding generation via sentence-transformers, and semantic search
 * against the pgvector store.
 * </p>
 */
@Component
public class AiWorkerClient {

    private final WebClient webClient;

    /**
     * Constructs the client with the AI Worker base URL
     * injected from application.yml (ai-worker.base-url).
     *
     * @param baseUrl the base URL of the Python AI Worker microservice
     */
    public AiWorkerClient(@Value("${ai-worker.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Sends a natural language question to the AI Worker and retrieves
     * the most semantically relevant code chunks from pgvector.
     *
     * @param question the natural language question about the codebase
     * @param topK     the number of most relevant chunks to retrieve
     * @return a list of {@link CodeChunk} objects ranked by similarity score
     */
    public List<CodeChunk> query(String question, int topK) {
        Map<String, Object> request = Map.of(
                "question", question,
                "top_k", topK
        );

        Map<String, Object> response = webClient.post()
                .uri("/query")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

        return results.stream()
                .map(this::mapToCodeChunk)
                .toList();
    }

    /**
     * Triggers the AI Worker to walk and index a Java repository.
     * <p>
     * This should be called once before querying, and again whenever
     * the repository changes. The worker parses all .java files into
     * AST chunks, generates embeddings, and stores them in pgvector.
     * </p>
     *
     * @param repoPath the absolute path to the Java repository on disk
     * @return a status string confirming successful indexing
     */
    public String index(String repoPath) {
        Map<String, Object> request = Map.of("repo_path", repoPath);

        Map<String, Object> response = webClient.post()
                .uri("/index")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        return response.get("status").toString();
    }

    /**
     * Maps a raw JSON response map from the AI Worker
     * into a typed {@link CodeChunk} domain object.
     * <p>
     * Handles the snake_case to camelCase translation between
     * the Python response and the Java model.
     * </p>
     *
     * @param map the raw deserialized JSON map from the AI Worker response
     * @return a populated {@link CodeChunk} object
     */
    private CodeChunk mapToCodeChunk(Map<String, Object> map) {
        CodeChunk chunk = new CodeChunk();
        chunk.setFilePath((String) map.get("file_path"));
        chunk.setClassName((String) map.get("class_name"));
        chunk.setMethodName((String) map.get("method_name"));
        chunk.setChunkType((String) map.get("chunk_type"));
        chunk.setContent((String) map.get("content"));
        chunk.setSimilarity(((Number) map.get("similarity")).doubleValue());
        return chunk;
    }
}