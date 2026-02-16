package com.stilog.documentation.service;

import java.util.List;
import java.util.Map;

import com.stilog.documentation.model.dto.DocumentProcessingResponse;
import com.stilog.vpimodel.objects.Entity;
import com.stilog.vpimodel.objects.Parameters;

public interface DocumentService {
    DocumentProcessingResponse processTemplates(
            String outputPath,
            Map<Entity, List<Parameters>> paramToInsert
        );
}
