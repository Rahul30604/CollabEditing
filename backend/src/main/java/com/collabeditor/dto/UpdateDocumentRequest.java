package com.collabeditor.dto;

import lombok.Data;

@Data
public class UpdateDocumentRequest {

    private String title;

    private String content;
}
