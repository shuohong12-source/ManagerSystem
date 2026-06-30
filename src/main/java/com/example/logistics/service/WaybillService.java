package com.example.logistics.service;

import com.example.logistics.mapper.WaybillMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class WaybillService {
    private static final BigDecimal BASE_RATE = new BigDecimal("0.08");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.06");

    private final WaybillMapper waybillMapper;

    public WaybillService(WaybillMapper waybillMapper) {
        this.waybillMapper = waybillMapper;
    }

    public List<Map<String, Object>> cities() {
        return waybillMapper.selectCities();
    }

    public List<Map<String, Object>> regions() {
        return waybillMapper.selectRegions();
    }

    public List<Map<String, Object>> goods() {
        return waybillMapper.selectGoods();
    }

    public List<Map<String, Object>> warehouses() {
        return waybillMapper.selectWarehouses();
    }

    public List<Map<String, Object>> search(String status, Long regionId, LocalDate start, LocalDate end) {
        return waybillMapper.search(
                StringUtils.hasText(status) ? status : null,
                regionId,
                start == null ? null : start.atStartOfDay(),
                end == null ? null : end.plusDays(1).atStartOfDay()
        );
    }

    public List<Map<String, Object>> items(Long waybillId) {
        return waybillMapper.selectItems(waybillId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Long create(String priority, String clerk, Long destinationCityId, String remark,
                       List<Long> goodsIds, List<Long> warehouseIds, List<Integer> quantities) {
        if (goodsIds == null || goodsIds.isEmpty()) {
            throw new IllegalArgumentException("至少需要添加一条运单项");
        }
        Map<String, Object> waybill = new LinkedHashMap<>();
        waybill.put("createDate", LocalDateTime.now());
        waybill.put("priority", priority);
        waybill.put("clerk", clerk);
        waybill.put("destinationCityId", destinationCityId);
        waybill.put("remark", remark);
        waybillMapper.insertWaybill(waybill);
        Long waybillId = ((Number) Objects.requireNonNull(waybill.get("waybillid"))).longValue();
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < goodsIds.size(); i++) {
            Long goodsId = goodsIds.get(i);
            Long whId = warehouseIds.get(i);
            Integer quantity = quantities.get(i);
            if (goodsId == null || whId == null || quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("第 " + (i + 1) + " 行运单项不完整");
            }
            BigDecimal declaredPrice = declaredPrice(goodsId);
            BigDecimal premiumRate = premiumRate(priority);
            Freight freight = freight(declaredPrice, quantity, premiumRate);

            int affected = waybillMapper.freezeInventory(whId, goodsId, quantity);
            if (affected != 1) {
                throw new IllegalStateException("库存不足，仓库 " + whId + " 商品 " + goodsId + " 无法扣减 " + quantity + " 件");
            }

            waybillMapper.insertWaybillItem(waybillId, goodsId, whId, quantity,
                    freight.baseFreight(), premiumRate, TAX_RATE, freight.lineFreight());
            waybillMapper.insertStockTxn(whId, goodsId, waybillId, -quantity,
                    "出库冻结", clerk, "创建运单自动冻结库存");
            total = total.add(freight.lineFreight());
        }
        waybillMapper.updateWaybillFreight(waybillId, total);
        return waybillId;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void changeStatus(Long waybillId, String targetStatus) {
        String current = waybillMapper.selectStatus(waybillId);
        if (Objects.equals(current, targetStatus)) {
            return;
        }
        if ("已取消".equals(current) || "已签收".equals(current)) {
            throw new IllegalStateException("已取消或已签收的运单不能再次变更状态");
        }
        List<Map<String, Object>> rows = items(waybillId);
        if ("已取消".equals(targetStatus)) {
            for (Map<String, Object> item : rows) {
                int quantity = ((Number) item.get("quantity")).intValue();
                waybillMapper.releaseInventory(item.get("whid"), item.get("goodsid"), quantity);
                waybillMapper.insertStockTxn(item.get("whid"), item.get("goodsid"), waybillId, quantity,
                        "取消释放", null, "取消运单释放冻结库存");
            }
        } else if ("已签收".equals(targetStatus)) {
            for (Map<String, Object> item : rows) {
                int quantity = ((Number) item.get("quantity")).intValue();
                waybillMapper.shipInventory(item.get("whid"), item.get("goodsid"), quantity);
                waybillMapper.insertStockTxn(item.get("whid"), item.get("goodsid"), waybillId, -quantity,
                        "签收出库", null, "签收后确认出库");
            }
        }
        waybillMapper.updateStatus(waybillId, targetStatus);
    }

    private BigDecimal declaredPrice(Long goodsId) {
        BigDecimal declaredPrice = waybillMapper.selectDeclaredPrice(goodsId);
        if (declaredPrice == null) {
            throw new IllegalArgumentException("商品不存在: " + goodsId);
        }
        return declaredPrice;
    }

    private BigDecimal premiumRate(String priority) {
        return switch (priority) {
            case "加急" -> new BigDecimal("0.20");
            case "特急" -> new BigDecimal("0.50");
            default -> BigDecimal.ZERO;
        };
    }

    private Freight freight(BigDecimal declaredPrice, int quantity, BigDecimal premiumRate) {
        BigDecimal amount = declaredPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal base = amount.multiply(BASE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal premium = base.multiply(premiumRate);
        BigDecimal tax = amount.multiply(TAX_RATE);
        BigDecimal line = base.add(premium).add(tax).setScale(2, RoundingMode.HALF_UP);
        return new Freight(base, line);
    }

    private record Freight(BigDecimal baseFreight, BigDecimal lineFreight) {
    }
}
