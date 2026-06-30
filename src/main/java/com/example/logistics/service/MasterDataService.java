package com.example.logistics.service;

import com.example.logistics.mapper.MasterDataMapper;
import com.example.logistics.model.MasterDefinition;
import com.example.logistics.model.MasterField;
import com.example.logistics.model.OptionItem;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Service
public class MasterDataService {
    private final MasterDataMapper masterDataMapper;
    private final Map<String, MasterDefinition> definitions = new LinkedHashMap<>();

    public MasterDataService(MasterDataMapper masterDataMapper) {
        this.masterDataMapper = masterDataMapper;
        definitions.put("regions", new MasterDefinition("regions", "枢纽区域", "hub_region",
                List.of("regionid"),
                List.of(MasterField.text("name", "区域名称", true),
                        MasterField.text("remark", "备注", false))));
        definitions.put("cities", new MasterDefinition("cities", "城市", "city",
                List.of("cityid"),
                List.of(MasterField.text("name", "城市名称", true),
                        MasterField.select("regionid", "所属枢纽区域", "regions", true),
                        MasterField.text("remark", "备注", false))));
        definitions.put("goods", new MasterDefinition("goods", "商品", "goods",
                List.of("goodsid"),
                List.of(MasterField.text("name", "商品名称", true),
                        MasterField.text("category", "类目", true),
                        MasterField.text("brand", "品牌", false),
                        MasterField.money("declared_price", "申报单价", true),
                        MasterField.text("remark", "备注", false))));
        definitions.put("suppliers", new MasterDefinition("suppliers", "供应商", "supplier",
                List.of("suppid"),
                List.of(MasterField.text("name", "供应商名称", true),
                        MasterField.text("address", "地址", false),
                        MasterField.select("cityid", "所属城市", "cities", true),
                        MasterField.text("phone", "电话", false),
                        MasterField.text("remark", "备注", false))));
        definitions.put("warehouses", new MasterDefinition("warehouses", "仓库", "warehouse",
                List.of("whid"),
                List.of(MasterField.text("name", "仓库名称", true),
                        MasterField.money("capacity", "库容体积", true),
                        MasterField.select("cityid", "所属城市", "cities", true),
                        MasterField.text("contact", "联系人", false),
                        MasterField.text("remark", "备注", false))));
        definitions.put("inventory", new MasterDefinition("inventory", "库存", "inventory",
                List.of("whid", "goodsid"),
                List.of(MasterField.select("whid", "仓库", "warehouses", true),
                        MasterField.select("goodsid", "商品", "goods", true),
                        MasterField.number("quantity", "可用库存", true),
                        MasterField.number("frozen_quantity", "冻结库存", true),
                        MasterField.number("safety_stock", "安全库存", true))));
    }

    public List<MasterDefinition> definitions() {
        return new ArrayList<>(definitions.values());
    }

    public MasterDefinition definition(String type) {
        MasterDefinition definition = definitions.get(type);
        if (definition == null) {
            throw new IllegalArgumentException("未知基础数据类型: " + type);
        }
        return definition;
    }

    public List<Map<String, Object>> findAll(MasterDefinition definition, String keyword) {
        String effectiveKeyword = StringUtils.hasText(keyword) && hasColumn(definition, "name") ? keyword.trim() : null;
        return masterDataMapper.findAll(definition.tableName(), effectiveKeyword, definition.idColumns().get(0));
    }

    public Map<String, Object> findById(MasterDefinition definition, String idValue) {
        IdParts parts = idParts(definition, idValue);
        List<Map<String, Object>> rows = masterDataMapper.findById(definition.tableName(), parts.conditions());
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    public void save(MasterDefinition definition, Map<String, String> form) {
        String editId = form.get("_editId");
        if (StringUtils.hasText(editId)) {
            update(definition, editId, form);
        } else {
            insert(definition, form);
        }
    }

    public void delete(MasterDefinition definition, String idValue) {
        IdParts parts = idParts(definition, idValue);
        masterDataMapper.delete(definition.tableName(), parts.conditions());
    }

    public Map<String, List<OptionItem>> optionMap() {
        Map<String, List<OptionItem>> options = new LinkedHashMap<>();
        options.put("regions", masterDataMapper.selectRegionOptions());
        options.put("cities", masterDataMapper.selectCityOptions());
        options.put("goods", masterDataMapper.selectGoodsOptions());
        options.put("warehouses", masterDataMapper.selectWarehouseOptions());
        return options;
    }

    public String rowId(MasterDefinition definition, Map<String, Object> row) {
        StringJoiner joiner = new StringJoiner("_");
        for (String column : definition.idColumns()) {
            joiner.add(String.valueOf(row.get(column)));
        }
        return joiner.toString();
    }

    public Object displayValue(MasterField field, Map<String, Object> row, Map<String, List<OptionItem>> options) {
        Object rawValue = row.get(field.name());
        if (!"select".equals(field.type()) || rawValue == null) {
            return rawValue;
        }
        return options.getOrDefault(field.refType(), List.of()).stream()
                .filter(option -> String.valueOf(option.id()).equals(String.valueOf(rawValue)))
                .map(OptionItem::label)
                .findFirst()
                .orElse(rawValue.toString());
    }

    private void insert(MasterDefinition definition, Map<String, String> form) {
        List<String> columns = definition.fields().stream().map(MasterField::name).toList();
        List<Object> values = columns.stream().map(c -> valueFor(definition, c, form.get(c))).toList();
        masterDataMapper.insert(definition.tableName(), columns, values);
    }

    private void update(MasterDefinition definition, String editId, Map<String, String> form) {
        List<String> columns = definition.fields().stream()
                .map(MasterField::name)
                .filter(c -> !definition.idColumns().contains(c))
                .toList();
        IdParts parts = idParts(definition, editId);
        List<Map<String, Object>> assignments = columns.stream()
                .map(c -> columnValue(c, valueFor(definition, c, form.get(c))))
                .toList();
        masterDataMapper.update(definition.tableName(), assignments, parts.conditions());
    }

    private boolean hasColumn(MasterDefinition definition, String column) {
        return definition.fields().stream().anyMatch(f -> f.name().equals(column));
    }

    private Object valueFor(MasterDefinition definition, String column, String raw) {
        MasterField field = definition.fields().stream()
                .filter(f -> f.name().equals(column))
                .findFirst()
                .orElseThrow();
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return switch (field.type()) {
            case "number" -> Integer.parseInt(raw);
            case "money" -> new BigDecimal(raw);
            case "select" -> Long.parseLong(raw);
            default -> raw.trim();
        };
    }

    private IdParts idParts(MasterDefinition definition, String idValue) {
        String[] values = idValue.split("_");
        if (values.length != definition.idColumns().size()) {
            throw new IllegalArgumentException("主键格式错误");
        }
        List<Map<String, Object>> conditions = new ArrayList<>();
        for (int i = 0; i < definition.idColumns().size(); i++) {
            conditions.add(columnValue(definition.idColumns().get(i), Long.parseLong(values[i])));
        }
        return new IdParts(conditions);
    }

    private Map<String, Object> columnValue(String column, Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("column", column);
        result.put("value", value);
        return result;
    }

    private record IdParts(List<Map<String, Object>> conditions) {
    }
}
