package com.example.logistics.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface RecommendationMapper {
    List<Map<String, Object>> recommend(@Param("destinationCityId") Long destinationCityId,
                                        @Param("goodsId") Long goodsId);
}
