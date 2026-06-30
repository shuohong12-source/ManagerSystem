package com.example.logistics.model;

public record MasterField(
        String name,
        String label,
        String type,
        String refType,
        boolean required
) {
    public static MasterField text(String name, String label, boolean required) {
        return new MasterField(name, label, "text", null, required);
    }

    public static MasterField number(String name, String label, boolean required) {
        return new MasterField(name, label, "number", null, required);
    }

    public static MasterField money(String name, String label, boolean required) {
        return new MasterField(name, label, "money", null, required);
    }

    public static MasterField select(String name, String label, String refType, boolean required) {
        return new MasterField(name, label, "select", refType, required);
    }
}
