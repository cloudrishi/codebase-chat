package com.cloudrishi.codebasechat.model;

import lombok.Data;

@Data
public class QueryRequest {
    private String question;
    private int topK = 5;
}