package com.collabeditor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiQuestionRequest {

    @NotBlank(message = "Question is required")
    private String question;
}
