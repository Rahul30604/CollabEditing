package com.collabeditor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiSearchRequest {

    @NotBlank(message = "Query is required")
    private String query;
}
