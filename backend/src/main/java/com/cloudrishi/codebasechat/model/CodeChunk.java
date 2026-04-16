package com.cloudrishi.codebasechat.model;

import lombok.Data;

@Data
public class CodeChunk {
    private String filePath;
    private String className;
    private String methodName;
    private String chunkType;
    private String content;
    private double similarity;
}