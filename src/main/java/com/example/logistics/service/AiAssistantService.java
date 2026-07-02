package com.example.logistics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiAssistantService {
    private static final int SUMMARY_LIMIT = 20;
    private static final int DETAIL_LIMIT = 120;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Value("${ai.openai.model:gpt-4.1-mini}")
    private String model;

    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    public AiAssistantService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> chat(String message) {
        String question = message == null ? "" : message.trim();
        if (!StringUtils.hasText(question)) {
            return Map.of("answer", "你可以直接问我库存、商品、运单、仓库推荐或月度对账相关问题。", "mode", "empty");
        }
        boolean businessQuestion = isBusinessQuestion(question);
        boolean detailedQuestion = wantsDetailedListing(question);
        String context = businessQuestion ? databaseContext(question, detailedQuestion) : "";
        if (businessQuestion && wantsRawDataListing(question)) {
            return Map.of(
                    "answer", "按你的要求直接列出数据库明细：\n\n" + context,
                    "mode", "database"
            );
        }
        if (!StringUtils.hasText(apiKey)) {
            return Map.of(
                    "answer", fallbackAnswer(question, context, businessQuestion),
                    "mode", businessQuestion ? "database-fallback" : "chat-fallback"
            );
        }
        try {
            String answer = callAi(question, context, businessQuestion, detailedQuestion);
            return Map.of("answer", answer, "mode", businessQuestion ? "database" : "chat");
        } catch (Exception ex) {
            return Map.of(
                    "answer", "AI API 调用失败：" + ex.getMessage() + "\n\n" + apiFailureFallback(context, businessQuestion),
                    "mode", "error"
            );
        }
    }

    private boolean isBusinessQuestion(String question) {
        String text = question.toLowerCase();
        return text.matches(".*(数据库|数据|信息|明细|列表|全部|所有|查询|统计|库存|冻结|安全库存|商品|货物|仓库|运单|订单|物流|运费|费用|对账|枢纽|区域|城市|推荐|供应商|报表|低库存|高价值|签收|发运|运输).*");
    }

    private boolean wantsDetailedListing(String question) {
        return question.matches(".*(全部|所有|完整|明细|列表|列出|展示|显示|一览|都有哪些|有哪些|所有信息).*");
    }

    private boolean wantsRawDataListing(String question) {
        return question.matches(".*(显示|展示|列出|明细|列表|一览|所有信息|全部信息|全部数据|所有数据).*");
    }

    private String callAi(String question, String context, boolean businessQuestion, boolean detailedQuestion) throws Exception {
        String input = """
                你是智慧跨境物流供应链管理系统内置的 AI 助手。
                回答要求：
                1. 如果问题与业务数据库有关，只能基于【数据库上下文】回答，不要编造不存在的数据。
                2. 如果上下文不足，直接说明“当前数据不足以判断”，并给出可继续查询的方向。
                3. 如果问题与业务无关，就像普通聊天机器人一样自然回答。
                4. 如果用户要求“全部、所有、明细、列表、列出、显示”，必须尽量逐条列出上下文中对应的明细，不要只做概括。
                5. 如果上下文中写明“最多显示”，回答结尾说明结果受上下文行数限制。
                6. 其他问题用简洁中文回答，必要时用项目符号。

                【数据库上下文】
                %s

                【用户问题】
                %s
                """.formatted(businessQuestion ? context : "本问题未判定为业务数据库问题，不提供业务数据。", question);
        if (baseUrl.toLowerCase().contains("deepseek")) {
            return callChatCompletions(input, detailedQuestion);
        }
        return callResponses(input, detailedQuestion);
    }

    private String callResponses(String input, boolean detailedQuestion) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("input", input);
        payload.put("max_output_tokens", detailedQuestion ? 3000 : 1200);
        String json = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl.replaceAll("/$", "") + "/responses"))
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " " + response.body());
        }
        return extractText(response.body());
    }

    private String callChatCompletions(String input, boolean detailedQuestion) throws Exception {
        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", input);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", List.of(userMessage));
        payload.put("max_tokens", detailedQuestion ? 3000 : 1200);
        payload.put("stream", false);
        payload.put("thinking", Map.of("type", "disabled"));

        String json = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl.replaceAll("/$", "") + "/chat/completions"))
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " " + response.body());
        }
        return extractChatText(response.body());
    }

    private String extractText(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        if (root.hasNonNull("output_text")) {
            return root.get("output_text").asText();
        }
        StringBuilder text = new StringBuilder();
        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (content.isArray()) {
                    for (JsonNode part : content) {
                        if (part.hasNonNull("text")) {
                            text.append(part.get("text").asText()).append('\n');
                        }
                    }
                }
            }
        }
        String result = text.toString().trim();
        return StringUtils.hasText(result) ? result : "AI 已返回结果，但没有可展示的文本内容。";
    }

    private String extractChatText(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            String content = choices.get(0).path("message").path("content").asText();
            if (StringUtils.hasText(content)) {
                return content.trim();
            }
        }
        return "AI 已返回结果，但没有可展示的文本内容。";
    }


    private String fallbackAnswer(String question, String context, boolean businessQuestion) {
        if (businessQuestion) {
            return "当前未配置 AI API Key，我先把可用的数据库摘要列出来：\n\n" + context;
        }
        return "当前未配置 AI API Key，所以普通聊天能力暂不可用。你可以设置环境变量 OPENAI_API_KEY 后重启服务。";
    }

    private String apiFailureFallback(String context, boolean businessQuestion) {
        if (businessQuestion) {
            return "外部 AI 暂不可用，我先把可用的数据库摘要列出来：\n\n" + context;
        }
        return "外部 AI 暂不可用。请检查 OPENAI_MODEL 是否是当前 API Key 有权限访问的模型。";
    }

    private String databaseContext(String question, boolean detailedQuestion) {
        String text = question.toLowerCase();
        int limit = detailedQuestion ? DETAIL_LIMIT : SUMMARY_LIMIT;
        boolean allDataQuestion = detailedQuestion && text.matches(".*(全部|所有|完整|所有信息).*");
        boolean goodsQuestion = allDataQuestion || text.matches(".*(商品|货物|货品|高价值).*");
        boolean inventoryQuestion = allDataQuestion || text.matches(".*(库存|冻结|安全库存|低库存).*");
        boolean warehouseQuestion = allDataQuestion || text.matches(".*(仓库|城市|区域|枢纽).*");
        boolean supplierQuestion = allDataQuestion || text.matches(".*(供应商).*");
        boolean waybillQuestion = allDataQuestion || text.matches(".*(运单|订单|物流|运费|发运|签收|运输|状态|对账|费用).*");

        List<String> sections = new ArrayList<>();
        sections.add(section("系统概览", query("""
                SELECT
                    (SELECT COUNT(*) FROM waybill) AS waybill_count,
                    (SELECT COUNT(*) FROM goods) AS goods_count,
                    (SELECT COUNT(*) FROM warehouse) AS warehouse_count,
                    (SELECT COALESCE(SUM(total_freight), 0) FROM waybill) AS total_freight
                """, 1)));

        if (inventoryQuestion || !detailedQuestion) {
            sections.add(section("库存风险", query("""
                SELECT wh.name AS warehouse, g.name AS goods, g.brand,
                       inv.quantity AS available_quantity,
                       inv.frozen_quantity,
                       inv.safety_stock,
                       CASE
                         WHEN inv.quantity <= inv.frozen_quantity THEN '冻结压力'
                         WHEN inv.quantity <= inv.safety_stock THEN '低于安全库存'
                         ELSE '正常'
                       END AS status
                FROM inventory inv
                JOIN warehouse wh ON wh.whid = inv.whid
                JOIN goods g ON g.goodsid = inv.goodsid
                ORDER BY
                    CASE
                      WHEN inv.quantity <= inv.frozen_quantity THEN 0
                      WHEN inv.quantity <= inv.safety_stock THEN 1
                      ELSE 2
                    END,
                    inv.quantity ASC
                LIMIT %d
                """.formatted(limit), limit)));
        }

        if (inventoryQuestion) {
            sections.add(section("库存明细（最多显示前 " + limit + " 条）", query("""
                SELECT wh.whid, wh.name AS warehouse, g.goodsid, g.name AS goods, g.category, g.brand,
                       inv.quantity AS available_quantity, inv.frozen_quantity, inv.safety_stock,
                       inv.update_time
                FROM inventory inv
                JOIN warehouse wh ON wh.whid = inv.whid
                JOIN goods g ON g.goodsid = inv.goodsid
                ORDER BY wh.whid, g.goodsid
                LIMIT %d
                """.formatted(limit), limit)));
        }

        if (goodsQuestion || !detailedQuestion) {
            sections.add(section(detailedQuestion ? "商品明细（最多显示前 " + limit + " 条）" : "高价值商品", query("""
                SELECT goodsid, name, category, brand, declared_price, remark
                FROM goods
                ORDER BY %s
                LIMIT %d
                """.formatted(detailedQuestion ? "goodsid" : "declared_price DESC", detailedQuestion ? limit : 12), detailedQuestion ? limit : 12)));
        }

        if (warehouseQuestion || !detailedQuestion) {
            sections.add(section("仓库与区域" + (detailedQuestion ? "（最多显示前 " + limit + " 条）" : ""), query("""
                SELECT wh.name AS warehouse, c.name AS city, r.name AS region, wh.capacity, wh.contact
                FROM warehouse wh
                JOIN city c ON c.cityid = wh.cityid
                JOIN hub_region r ON r.regionid = c.regionid
                ORDER BY r.name, c.name, wh.name
                LIMIT %d
                """.formatted(limit), limit)));
            sections.add(section("城市与枢纽区域（最多显示前 " + limit + " 条）", query("""
                SELECT c.cityid, c.name AS city, r.regionid, r.name AS region, c.remark
                FROM city c
                JOIN hub_region r ON r.regionid = c.regionid
                ORDER BY r.regionid, c.cityid
                LIMIT %d
                """.formatted(limit), limit)));
        }

        if (supplierQuestion) {
            sections.add(section("供应商明细（最多显示前 " + limit + " 条）", query("""
                SELECT s.suppid, s.name AS supplier, c.name AS city, r.name AS region,
                       s.address, s.phone, s.remark
                FROM supplier s
                JOIN city c ON c.cityid = s.cityid
                JOIN hub_region r ON r.regionid = c.regionid
                ORDER BY s.suppid
                LIMIT %d
                """.formatted(limit), limit)));
        }

        if (waybillQuestion || !detailedQuestion) {
            sections.add(section(detailedQuestion ? "运单明细（最多显示前 " + limit + " 条）" : "最近运单", query("""
                SELECT w.waybillid, w.status, w.priority, w.total_freight, w.create_date,
                       w.clerk,
                       c.name AS destination_city, r.name AS destination_region,
                       COALESCE(SUM(i.quantity), 0) AS item_quantity,
                       w.remark
                FROM waybill w
                JOIN city c ON c.cityid = w.destination_cityid
                JOIN hub_region r ON r.regionid = c.regionid
                LEFT JOIN waybill_item i ON i.waybillid = w.waybillid
                GROUP BY w.waybillid, w.status, w.priority, w.total_freight, w.create_date, w.clerk, c.name, r.name, w.remark
                ORDER BY w.create_date DESC
                LIMIT %d
                """.formatted(detailedQuestion ? limit : 12), detailedQuestion ? limit : 12)));
            sections.add(section("运单状态分布", query("""
                SELECT status, COUNT(*) AS count_value
                FROM waybill
                GROUP BY status
                ORDER BY FIELD(status, '待发运', '运输中', '已签收', '已取消')
                """, 8)));
        }

        if (waybillQuestion && detailedQuestion) {
            sections.add(section("运单项明细（最多显示前 " + limit + " 条）", query("""
                SELECT i.itemid, i.waybillid, g.name AS goods, wh.name AS warehouse,
                       i.quantity, i.base_freight, i.premium_rate, i.tax, i.line_freight
                FROM waybill_item i
                JOIN goods g ON g.goodsid = i.goodsid
                JOIN warehouse wh ON wh.whid = i.whid
                ORDER BY i.waybillid DESC, i.itemid
                LIMIT %d
                """.formatted(limit), limit)));
        }

        sections.add(section("用户问题", List.of(Map.of("question", question))));
        return String.join("\n\n", sections);
    }

    private List<Map<String, Object>> query(String sql, int limit) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            return rows.size() > limit ? rows.subList(0, limit) : rows;
        } catch (Exception ex) {
            return List.of(Map.of("error", ex.getMessage()));
        }
    }

    private String section(String title, List<Map<String, Object>> rows) {
        StringBuilder builder = new StringBuilder("## ").append(title).append('\n');
        if (rows.isEmpty()) {
            return builder.append("暂无数据").toString();
        }
        for (Map<String, Object> row : rows) {
            builder.append("- ");
            row.forEach((key, value) -> builder.append(key).append("=").append(value).append("; "));
            builder.append('\n');
        }
        return builder.toString().trim();
    }
}
