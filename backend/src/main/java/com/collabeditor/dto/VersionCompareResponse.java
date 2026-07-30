package com.collabeditor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VersionCompareResponse {
    private VersionResponse versionA;
    private VersionResponse versionB;
    private String contentA;
    private String contentB;
}
