package org.zc.ingest.parser;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class EventPayloadMetadataHelper {
    public Map<String, Object> attachMetadata(Map<String, Object> payload, Map<String, Object> metadata) {
        Map<String, Object> mergedPayload = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
        Map<String, Object> mergedMetadata = new LinkedHashMap<>();

        Object existingMeta = mergedPayload.get("_meta");
        if (existingMeta instanceof Map<?, ?> existingMap) {
            for (Map.Entry<?, ?> entry : existingMap.entrySet()) {
                if (entry.getKey() != null) {
                    mergedMetadata.put(entry.getKey().toString(), entry.getValue());
                }
            }
        }
        mergedMetadata.putAll(metadata);
        mergedPayload.put("_meta", mergedMetadata);
        return mergedPayload;
    }
}
