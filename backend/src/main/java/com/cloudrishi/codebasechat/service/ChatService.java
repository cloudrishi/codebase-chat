package com.cloudrishi.codebasechat.service;

import com.cloudrishi.codebasechat.client.AiWorkerClient;
import com.cloudrishi.codebasechat.model.CodeChunk;
import com.cloudrishi.codebasechat.model.QueryRequest;
import com.cloudrishi.codebasechat.model.QueryResponse;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.SystemMessage;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Core service responsible for orchestrating the RAG pipeline.
 * <p>
 * Coordinates between the AI Worker (retrieval) and Ollama (generation)
 * to produce context-aware answers about the indexed Java codebase.
 * This is the "Augmented Generation" part of RAG — it retrieves
 * relevant code chunks and feeds them as context to the LLM.
 * </p>
 */
@Service
public class ChatService {

    private final AiWorkerClient aiWorkerClient;
    private final OllamaChatModel ollamaChatModel;

    /**
     * Constructor injection — preferred over field injection
     * for testability and immutability.
     *
     * @param aiWorkerClient  client for the Python AI Worker microservice
     * @param ollamaChatModel LangChain4j Ollama chat model bean
     */
    public ChatService(AiWorkerClient aiWorkerClient,
                       OllamaChatModel ollamaChatModel) {
        this.aiWorkerClient = aiWorkerClient;
        this.ollamaChatModel = ollamaChatModel;
    }

    /**
     * Executes the full RAG pipeline for a given question.
     * <ol>
     *   <li>Retrieves the most relevant code chunks from pgvector
     *       via the AI Worker</li>
     *   <li>Builds a prompt with the retrieved chunks as context</li>
     *   <li>Sends the prompt to Ollama for answer generation</li>
     *   <li>Returns the answer along with the source chunks</li>
     * </ol>
     *
     * @param request the query request containing the question and topK
     * @return a {@link QueryResponse} containing the answer and relevant chunks
     */
    public QueryResponse chat(QueryRequest request) {
        List<CodeChunk> relevantChunks = aiWorkerClient.query(
                request.getQuestion(),
                request.getTopK()
        );

        String context = buildContext(relevantChunks);
        String answer = generateAnswer(request.getQuestion(), context);

        return QueryResponse.builder()
                .question(request.getQuestion())
                .answer(answer)
                .relevantChunks(relevantChunks)
                .build();
    }

    /**
     * Triggers indexing of a Java repository via the AI Worker.
     *
     * @param repoPath absolute path to the Java repository on disk
     * @return status message from the AI Worker
     */
    public String index(String repoPath) {
        return aiWorkerClient.index(repoPath);
    }

    /**
     * Builds a structured context string from retrieved code chunks
     * to be injected into the LLM prompt.
     *
     * @param chunks the list of relevant code chunks from pgvector
     * @return a formatted string containing all chunk contents
     */
    private String buildContext(List<CodeChunk> chunks) {
        StringBuilder context = new StringBuilder();
        for (CodeChunk chunk : chunks) {
            context.append("### ")
                    .append(chunk.getChunkType().toUpperCase())
                    .append(": ")
                    .append(chunk.getClassName());
            if (chunk.getMethodName() != null) {
                context.append(".").append(chunk.getMethodName());
            }
            context.append("\n")
                    .append(chunk.getContent())
                    .append("\n\n");
        }
        return context.toString();
    }

    /**
     * Sends the question and retrieved context to Ollama
     * and returns the generated answer.
     *
     * @param question the original natural language question
     * @param context  the formatted code chunks as context
     * @return the LLM generated answer string
     */
    private String generateAnswer(String question, String context) {
        SystemMessage systemMessage = SystemMessage.from(
                "You are an expert Java architect assistant. " +
                        "Answer questions about the codebase using ONLY " +
                        "the provided code context. If the answer cannot " +
                        "be found in the context, say so clearly."
        );

        UserMessage userMessage = UserMessage.from(
                "Context:\n" + context +
                        "\n\nQuestion: " + question
        );

        ChatResponse response = ollamaChatModel.chat(
                systemMessage, userMessage
        );

        return response.aiMessage().text();
    }
}