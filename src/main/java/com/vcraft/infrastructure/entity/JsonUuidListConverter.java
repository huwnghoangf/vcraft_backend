package com.vcraft.infrastructure.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;
import java.util.UUID;

@Converter
public class JsonUuidListConverter implements AttributeConverter<List<UUID>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<UUID> attribute) {
        if (attribute == null) return "[]";
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Không thể mã hóa danh sách JSON", e);
        }
    }

    @Override
    public List<UUID> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return List.of();
        try {
            return MAPPER.readValue(dbData, new TypeReference<List<UUID>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Danh sách JSON trong dữ liệu không hợp lệ", e);
        }
    }
}
