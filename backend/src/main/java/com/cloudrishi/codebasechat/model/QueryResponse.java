package com.cloudrishi.codebasechat.model;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class QueryResponse {
    private String question;
    private String answer;
    private List<CodeChunk> relevantChunks;
}