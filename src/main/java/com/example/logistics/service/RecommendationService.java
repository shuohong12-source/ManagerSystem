package com.example.logistics.service;

import com.example.logistics.mapper.RecommendationMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {
    private final RecommendationMapper recommendationMapper;
    public RecommendationService(RecommendationMapper recommendationMapper) {
        this.recommendationMapper = recommendationMapper;
    }
    public List<Map<String, Object>> recommend(Long destinationCityId, Long goodsId) {
        if (destinationCityId == null || goodsId == null) {
            return List.of();
        }
        return recommendationMapper.recommend(destinationCityId, goodsId);
    }
}
