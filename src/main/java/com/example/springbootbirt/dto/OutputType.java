package com.example.springbootbirt.dto;

import org.eclipse.birt.report.engine.api.IRenderOption;

public enum OutputType {
    HTML(IRenderOption.OUTPUT_FORMAT_HTML),
    PDF(IRenderOption.OUTPUT_FORMAT_PDF),
    XLSX("xlsx"),
    INVALID("invalid");

    String reportTypeName;
    OutputType(String reportTypeName) {
        this.reportTypeName = reportTypeName;
    }

    public static OutputType from(String text) {
        for (OutputType output : values()) {
            if(output.reportTypeName.equalsIgnoreCase(text)) {
                return output;
            }
        }
        return INVALID;
    }
}