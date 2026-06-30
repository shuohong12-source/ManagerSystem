package com.example.logistics.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ReportMapper {
    List<Map<String, Object>> monthly(@Param("month") String month);
}
