package com.example.logistics.model;

import java.util.List;

public record MasterDefinition(
        String type,
        String title,
        String tableName,
        List<String> idColumns,
        List<MasterField> fields
) {
    public boolean compositeId() {
        return idColumns.size() > 1;
    }
}
