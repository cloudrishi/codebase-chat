package com.cloudrishi.codebasechat.client;

import com.cloudrishi.codebasechat.model.CodeChunk;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AiWorkerClient null guards and response mapping.
 * Uses MockWebServer to simulate the Python AI Worker HTTP responses
 * without requiring a live service.
 *
 * Dependency required in pom.xml:
 * <dependency>
 *     <groupId>com.squareup.okhttp3</groupId>
 *     <artifactId>mockwebserver</artifactId>
 *     <scope>test</scope>
 * </dependency>
 */
class AiWorkerClientTest {

    private MockWebServer mockWebServer;
    private AiWorkerClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        client = new AiWorkerClient(mockWebServer.url("/").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // --- query() null guard tests ---

    @Test
    void query_returnsEmptyList_whenResponseBodyIsEmpty() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{}")
                .addHeader("Content-Type", "application/json"));

        List<CodeChunk> result = client.query("how does login work?", 5);

        assertThat(result).isEmpty();
    }

    @Test
    void query_returnsEmptyList_whenResultsKeyMissing() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"question\": \"how does login work?\"}")
                .addHeader("Content-Type", "application/json"));

        List<CodeChunk> result = client.query("how does login work?", 5);

        assertThat(result).isEmpty();
    }

    @Test
    void query_returnsEmptyList_whenResultsIsNotAList() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"results\": \"unexpected string\"}")
                .addHeader("Content-Type", "application/json"));

        List<CodeChunk> result = client.query("how does login work?", 5);

        assertThat(result).isEmpty();
    }

    @Test
    void query_returnsEmptyList_whenResultsIsEmptyArray() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"results\": []}")
                .addHeader("Content-Type", "application/json"));

        List<CodeChunk> result = client.query("how does login work?", 5);

        assertThat(result).isEmpty();
    }

    @Test
    void query_returnsMappedChunks_whenResponseIsValid() {
        String body = """
                {
                  "question": "how does login work?",
                  "results": [
                    {
                      "file_path": "src/UserService.java",
                      "class_name": "UserService",
                      "method_name": "login",
                      "chunk_type": "method",
                      "content": "public AuthResponse login(LoginRequest req) { ... }",
                      "similarity": 0.91
                    }
                  ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

        List<CodeChunk> result = client.query("how does login work?", 5);

        assertThat(result).hasSize(1);
        CodeChunk chunk = result.get(0);
        assertThat(chunk.getFilePath()).isEqualTo("src/UserService.java");
        assertThat(chunk.getClassName()).isEqualTo("UserService");
        assertThat(chunk.getMethodName()).isEqualTo("login");
        assertThat(chunk.getChunkType()).isEqualTo("method");
        assertThat(chunk.getSimilarity()).isEqualTo(0.91);
    }

    @Test
    void query_filtersOutNonMapItems_inResultsList() {
        String body = """
                {
                  "results": [
                    "not a map",
                    {
                      "file_path": "src/Foo.java",
                      "class_name": "Foo",
                      "method_name": "bar",
                      "chunk_type": "method",
                      "content": "void bar() {}",
                      "similarity": 0.75
                    }
                  ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

        List<CodeChunk> result = client.query("what does bar do?", 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClassName()).isEqualTo("Foo");
    }

    // --- index() null guard tests ---

    @Test
    void index_returnsFallbackMessage_whenServerReturns500() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("internal server error")
                .addHeader("Content-Type", "text/plain"));

        String result = client.index("/some/repo");

        assertThat(result).isEqualTo("indexing failed — no response from AI worker");
    }

    @Test
    void index_returnsUnknownStatus_whenStatusKeyMissing() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"total_chunks\": 42}")
                .addHeader("Content-Type", "application/json"));

        String result = client.index("/some/repo");

        assertThat(result).isEqualTo("unknown status");
    }

    @Test
    void index_returnsStatus_whenPresentInResponse() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"total_chunks\": 42, \"status\": \"indexed successfully\"}")
                .addHeader("Content-Type", "application/json"));

        String result = client.index("/some/repo");

        assertThat(result).isEqualTo("indexed successfully");
    }
}
