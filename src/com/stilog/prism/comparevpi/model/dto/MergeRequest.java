package com.stilog.prism.comparevpi.model.dto;

import com.stilog.prism.vpimodel.objects.Entity;
import com.stilog.prism.vpimodel.objects.Parameters;
import com.stilog.prism.vpimodel.vpsettings.FileDatas;

public class MergeRequest {
	
	public enum MergeType {
        MERGE_PARAMETER,
        MERGE_ENTITY,
        REPLACE_PARAMETER,
        REPLACE_ENTITY
    }
    
    private final MergeType type;
    private final FileDatas fileData;
    private final Entity entity;
    private final Parameters parameter;
    private final Object targetObject; // Pour le mode Replace
    
    // Constructor pour merge de Parameter
    public MergeRequest(FileDatas fileDatas, Entity entity, Parameters parameter) {
        this.type = MergeType.MERGE_PARAMETER;
        this.fileData = fileDatas;
        this.entity = entity;
        this.parameter = parameter;
        this.targetObject = null;
    }
    
    // Constructor pour merge de Entity
    public MergeRequest(FileDatas fileData, Entity entity) {
        this.type = MergeType.MERGE_ENTITY;
        this.fileData = fileData;
        this.entity = entity;
        this.parameter = null;
        this.targetObject = null;
    }
    
    // Constructor pour replace de Parameter
    public MergeRequest(FileDatas fileData, Entity entity, Parameters source, Parameters target) {
        this.type = MergeType.REPLACE_PARAMETER;
        this.fileData = fileData;
        this.entity = entity;
        this.parameter = source;
        this.targetObject = target;
    }
    
    // Constructor pour replace de Entity
    public MergeRequest(FileDatas fileData, Entity source, Entity target) {
        this.type = MergeType.REPLACE_ENTITY;
        this.fileData = fileData;
        this.entity = source;
        this.parameter = null;
        this.targetObject = target;
    }
    
    // Getters
    public MergeType getType() { return type; }
    public FileDatas getFileData() { return fileData; }
    public Entity getEntity() { return entity; }
    public Parameters getParameter() { return parameter; }
    public Object getTargetObject() { return targetObject; }
}
