package com.example.logistics.mapper;

import com.example.logistics.model.OptionItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface MasterDataMapper {
    List<Map<String, Object>> findAll(@Param("tableName") String tableName,
                                      @Param("keyword") String keyword,
                                      @Param("orderColumn") String orderColumn);
    List<Map<String, Object>> findInventoryByGoodsName(@Param("keyword") String keyword);
    List<Map<String, Object>> findById(@Param("tableName") String tableName,
                                       @Param("conditions") List<Map<String, Object>> conditions);
    int insert(@Param("tableName") String tableName,
               @Param("columns") List<String> columns,
               @Param("values") List<Object> values);
    int update(@Param("tableName") String tableName,
               @Param("assignments") List<Map<String, Object>> assignments,
               @Param("conditions") List<Map<String, Object>> conditions);
    int delete(@Param("tableName") String tableName,
               @Param("conditions") List<Map<String, Object>> conditions);
    List<OptionItem> selectRegionOptions();
    List<OptionItem> selectCityOptions();
    List<OptionItem> selectGoodsOptions();
    List<OptionItem> selectWarehouseOptions();
}
