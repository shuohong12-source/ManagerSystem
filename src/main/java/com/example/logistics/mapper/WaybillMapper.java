package com.example.logistics.mapper;

import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface WaybillMapper {
    List<Map<String, Object>> selectCities();

    List<Map<String, Object>> selectRegions();

    List<Map<String, Object>> selectGoods();

    List<Map<String, Object>> selectWarehouses();

    List<Map<String, Object>> search(@Param("status") String status,
                                     @Param("regionId") Long regionId,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    List<Map<String, Object>> selectItems(@Param("waybillId") Long waybillId);

    int insertWaybill(Map<String, Object> waybill);

    BigDecimal selectDeclaredPrice(@Param("goodsId") Long goodsId);

    int freezeInventory(@Param("whId") Long whId,
                        @Param("goodsId") Long goodsId,
                        @Param("quantity") Integer quantity);

    int insertWaybillItem(@Param("waybillId") Long waybillId,
                          @Param("goodsId") Long goodsId,
                          @Param("whId") Long whId,
                          @Param("quantity") Integer quantity,
                          @Param("baseFreight") BigDecimal baseFreight,
                          @Param("premiumRate") BigDecimal premiumRate,
                          @Param("tax") BigDecimal tax,
                          @Param("lineFreight") BigDecimal lineFreight);

    int insertStockTxn(@Param("whId") Object whId,
                       @Param("goodsId") Object goodsId,
                       @Param("waybillId") Long waybillId,
                       @Param("changeQuantity") Integer changeQuantity,
                       @Param("txnType") String txnType,
                       @Param("operatorName") String operatorName,
                       @Param("remark") String remark);

    int updateWaybillFreight(@Param("waybillId") Long waybillId,
                             @Param("totalFreight") BigDecimal totalFreight);

    String selectStatus(@Param("waybillId") Long waybillId);

    int releaseInventory(@Param("whId") Object whId,
                         @Param("goodsId") Object goodsId,
                         @Param("quantity") Integer quantity);

    int shipInventory(@Param("whId") Object whId,
                      @Param("goodsId") Object goodsId,
                      @Param("quantity") Integer quantity);

    int updateStatus(@Param("waybillId") Long waybillId,
                     @Param("status") String status);
}
