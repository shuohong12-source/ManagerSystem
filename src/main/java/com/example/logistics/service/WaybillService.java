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

    public Map<String, Object> waybill(Long waybillId) {
        Map<String, Object> waybill = waybillMapper.selectWaybill(waybillId);
        if (waybill == null || waybill.isEmpty()) {
            throw new IllegalArgumentException("运单不存在: " + waybillId);
        }
        return waybill;
    }

    public List<Map<String, Object>> items(Long waybillId) {
        return waybillMapper.selectItems(waybillId);
    }

    public String status(Long waybillId) {
        return waybillMapper.selectStatus(waybillId);
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

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void updateWaybill(Long waybillId, String priority, String clerk, Long destinationCityId, String remark) {
        waybill(waybillId);
        if (!StringUtils.hasText(priority) || !StringUtils.hasText(clerk) || destinationCityId == null) {
            throw new IllegalArgumentException("优先级、跟单员和目的地不能为空");
        }
        int updated = waybillMapper.updateWaybillHeader(waybillId, priority, clerk.trim(), destinationCityId,
                StringUtils.hasText(remark) ? remark.trim() : null);
        if (updated != 1) {
            throw new IllegalStateException("运单不存在或已被删除");
        }
        waybillMapper.updateItemFreightsForPriority(waybillId, premiumRate(priority), TAX_RATE);
        refreshWaybillFreight(waybillId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void deleteWaybill(Long waybillId) {
        String current = waybillMapper.selectStatus(waybillId);
        if (current == null) {
            throw new IllegalArgumentException("运单不存在: " + waybillId);
        }
        List<Map<String, Object>> rows = items(waybillId);
        if (!"已取消".equals(current) && !"已签收".equals(current)) {
            for (Map<String, Object> item : rows) {
                int quantity = intValue(item.get("quantity"));
                int released = waybillMapper.releaseInventory(item.get("whid"), item.get("goodsid"), quantity);
                if (released == 1) {
                    waybillMapper.insertStockTxn(item.get("whid"), item.get("goodsid"), waybillId, quantity,
                            "取消释放", null, "删除运单释放冻结库存");
                }
            }
        }
        waybillMapper.deleteStockTxnsByWaybill(waybillId);
        int deleted = waybillMapper.deleteWaybill(waybillId);
        if (deleted != 1) {
            throw new IllegalStateException("运单不存在或已被删除");
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void updateItem(Long waybillId, Long itemId, Long goodsId, Long whId, Integer quantity) {
        ensureEditable(waybillId);
        if (goodsId == null || whId == null || quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("运单项商品、仓库和数量不能为空，数量必须大于 0");
        }
        Map<String, Object> oldItem = item(waybillId, itemId);
        Long oldWhId = longValue(oldItem.get("whid"));
        Long oldGoodsId = longValue(oldItem.get("goodsid"));
        int oldQuantity = intValue(oldItem.get("quantity"));

        int released = waybillMapper.releaseInventory(oldWhId, oldGoodsId, oldQuantity);
        int frozen = waybillMapper.freezeInventory(whId, goodsId, quantity);
        if (frozen != 1) {
            throw new IllegalStateException("新商品或仓库库存不足，无法冻结 " + quantity + " 件");
        }

        BigDecimal premiumRate = premiumRate(waybillMapper.selectPriority(waybillId));
        Freight freight = freight(declaredPrice(goodsId), quantity, premiumRate);
        int updated = waybillMapper.updateWaybillItem(waybillId, itemId, goodsId, whId, quantity,
                freight.baseFreight(), premiumRate, TAX_RATE, freight.lineFreight());
        if (updated != 1) {
            throw new IllegalStateException("运单项不存在或已被删除");
        }
        if (released == 1) {
            waybillMapper.insertStockTxn(oldWhId, oldGoodsId, waybillId, oldQuantity,
                    "取消释放", null, "修改运单项释放原冻结库存");
        }
        waybillMapper.insertStockTxn(whId, goodsId, waybillId, -quantity,
                "出库冻结", null, "修改运单项重新冻结库存");
        refreshWaybillFreight(waybillId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void deleteItem(Long waybillId, Long itemId) {
        ensureEditable(waybillId);
        if (waybillMapper.countItems(waybillId) <= 1) {
            throw new IllegalStateException("运单至少需要保留一条明细");
        }
        Map<String, Object> oldItem = item(waybillId, itemId);
        Long oldWhId = longValue(oldItem.get("whid"));
        Long oldGoodsId = longValue(oldItem.get("goodsid"));
        int oldQuantity = intValue(oldItem.get("quantity"));
        int released = waybillMapper.releaseInventory(oldWhId, oldGoodsId, oldQuantity);
        int deleted = waybillMapper.deleteWaybillItem(waybillId, itemId);
        if (deleted != 1) {
            throw new IllegalStateException("运单项不存在或已被删除");
        }
        if (released == 1) {
            waybillMapper.insertStockTxn(oldWhId, oldGoodsId, waybillId, oldQuantity,
                    "取消释放", null, "删除运单项释放冻结库存");
        }
        refreshWaybillFreight(waybillId);
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

    private void ensureEditable(Long waybillId) {
        String current = waybillMapper.selectStatus(waybillId);
        if (current == null) {
            throw new IllegalArgumentException("运单不存在: " + waybillId);
        }
        if ("已取消".equals(current) || "已签收".equals(current)) {
            throw new IllegalStateException("已取消或已签收的运单不能修改明细");
        }
    }

    private Map<String, Object> item(Long waybillId, Long itemId) {
        Map<String, Object> item = waybillMapper.selectItem(waybillId, itemId);
        if (item == null || item.isEmpty()) {
            throw new IllegalArgumentException("运单项不存在: " + itemId);
        }
        return item;
    }

    private void refreshWaybillFreight(Long waybillId) {
        waybillMapper.updateWaybillFreight(waybillId, waybillMapper.sumItemFreight(waybillId));
    }

    private Long longValue(Object value) {
        return ((Number) value).longValue();
    }

    private int intValue(Object value) {
        return ((Number) value).intValue();
    }

    private record Freight(BigDecimal baseFreight, BigDecimal lineFreight) {
    }
}
