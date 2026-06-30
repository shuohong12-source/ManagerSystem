package com.example.logistics.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface DashboardMapper {
    Long countWaybills();

    Long countGoods();

    Long countWarehouses();

    BigDecimal sumFreight();

    List<Map<String, Object>> selectRegionVolumes();

    List<Map<String, Object>> selectTopGoods();

    List<Map<String, Object>> selectLowInventory();

    List<Map<String, Object>> selectStatusDistribution();

    List<Map<String, Object>> selectMonthlyTrend();
}
