package com.example.logistics.service;

import com.example.logistics.mapper.DashboardMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {
    private final DashboardMapper dashboardMapper;
    public DashboardService(DashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }
    public Map<String, Object> overview() {
        return Map.of(
                "waybillCount", dashboardMapper.countWaybills(),
                "goodsCount", dashboardMapper.countGoods(),
                "warehouseCount", dashboardMapper.countWarehouses(),
                "freightTotal", dashboardMapper.sumFreight()
        );
    }
    public List<Map<String, Object>> regionVolumes() {
        return dashboardMapper.selectRegionVolumes()
                .stream()
                .map(row -> {
                    Map<String, Object> result = new LinkedHashMap<>(row);
                    Number totalQuantity = (Number) row.get("total_quantity");
                    int quantity = totalQuantity == null ? 0 : totalQuantity.intValue();
                    int barWidth = quantity == 0 ? 3 : Math.min(100, quantity / 3);
                    result.put("bar_width", barWidth);
                    return result;
                })
                .toList();
    }
    public Object topGoods() {
        return dashboardMapper.selectTopGoods();
    }
    public Object lowInventory() {
        return dashboardMapper.selectLowInventory();
    }
    public Object statusDistribution() {
        return dashboardMapper.selectStatusDistribution();
    }
    public Object monthlyTrend() {
        return dashboardMapper.selectMonthlyTrend();
    }
}
