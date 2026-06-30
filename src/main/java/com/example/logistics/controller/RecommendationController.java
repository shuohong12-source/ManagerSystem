package com.example.logistics.controller;

import com.example.logistics.service.RecommendationService;
import com.example.logistics.service.WaybillService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RecommendationController {
    private final RecommendationService recommendationService;
    private final WaybillService waybillService;
    public RecommendationController(RecommendationService recommendationService, WaybillService waybillService) {
        this.recommendationService = recommendationService;
        this.waybillService = waybillService;
    }
    @GetMapping("/recommendations")
    public String page(@RequestParam(required = false) Long destinationCityId,
                       @RequestParam(required = false) Long goodsId,
                       Model model) {
        model.addAttribute("cities", waybillService.cities());
        model.addAttribute("goods", waybillService.goods());
        model.addAttribute("destinationCityId", destinationCityId);
        model.addAttribute("goodsId", goodsId);
        model.addAttribute("results", recommendationService.recommend(destinationCityId, goodsId));
        return "recommendations";
    }
}
