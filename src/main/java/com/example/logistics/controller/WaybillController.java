package com.example.logistics.controller;

import com.example.logistics.service.WaybillService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
public class WaybillController {
    private final WaybillService waybillService;

    public WaybillController(WaybillService waybillService) {
        this.waybillService = waybillService;
    }

    @GetMapping("/waybills")
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) Long regionId,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                       Model model) {
        model.addAttribute("waybills", waybillService.search(status, regionId, start, end));
        model.addAttribute("regions", waybillService.regions());
        model.addAttribute("status", status);
        model.addAttribute("regionId", regionId);
        model.addAttribute("start", start);
        model.addAttribute("end", end);
        return "waybills";
    }

    @GetMapping("/waybills/new")
    public String createPage(Model model) {
        model.addAttribute("cities", waybillService.cities());
        model.addAttribute("goods", waybillService.goods());
        model.addAttribute("warehouses", waybillService.warehouses());
        return "waybill-form";
    }

    @PostMapping("/waybills")
    public String create(@RequestParam String priority,
                         @RequestParam String clerk,
                         @RequestParam Long destinationCityId,
                         @RequestParam(required = false) String remark,
                         @RequestParam List<Long> goodsid,
                         @RequestParam List<Long> whid,
                         @RequestParam List<Integer> quantity,
                         RedirectAttributes redirectAttributes) {
        try {
            Long waybillId = waybillService.create(priority, clerk, destinationCityId, remark, goodsid, whid, quantity);
            redirectAttributes.addFlashAttribute("success", "运单创建成功，编号：" + waybillId);
            return "redirect:/waybills";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "运单创建失败：" + ex.getMessage());
            return "redirect:/waybills/new";
        }
    }

    @GetMapping("/waybills/{id}/items")
    public String items(@PathVariable Long id, Model model) {
        model.addAttribute("items", waybillService.items(id));
        model.addAttribute("waybillId", id);
        return "waybill-items";
    }

    @PostMapping("/waybills/{id}/status")
    public String status(@PathVariable Long id,
                         @RequestParam String status,
                         RedirectAttributes redirectAttributes) {
        try {
            waybillService.changeStatus(id, status);
            redirectAttributes.addFlashAttribute("success", "状态已更新");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "状态更新失败：" + ex.getMessage());
        }
        return "redirect:/waybills";
    }
}
