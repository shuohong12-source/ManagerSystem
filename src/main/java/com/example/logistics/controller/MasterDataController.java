package com.example.logistics.controller;

import com.example.logistics.model.MasterDefinition;
import com.example.logistics.service.MasterDataService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
public class MasterDataController {
    private final MasterDataService masterDataService;
    public MasterDataController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }
    @GetMapping("/master/{type}")
    public String page(@PathVariable String type,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false) String editId,
                       Model model) {
        MasterDefinition definition = masterDataService.definition(type);
        model.addAttribute("definition", definition);
        model.addAttribute("definitions", masterDataService.definitions());
        model.addAttribute("rows", masterDataService.findAll(definition, q));
        model.addAttribute("options", masterDataService.optionMap());
        model.addAttribute("q", q);
        model.addAttribute("editId", editId);
        model.addAttribute("editRow", editId == null ? Map.of() : masterDataService.findById(definition, editId));
        model.addAttribute("rowIdService", masterDataService);
        model.addAttribute("cityMapRows", "cities".equals(type) ? masterDataService.findAll(definition, null) : List.of());
        return "master";
    }
    @PostMapping("/master/{type}/save")
    public String save(@PathVariable String type,
                       @RequestParam Map<String, String> form,
                       RedirectAttributes redirectAttributes) {
        MasterDefinition definition = masterDataService.definition(type);
        try {
            masterDataService.save(definition, form);
            redirectAttributes.addFlashAttribute("success", "保存成功");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "保存失败：" + ex.getMessage());
        }
        return "redirect:/master/" + type;
    }
    @PostMapping("/master/{type}/delete")
    public String delete(@PathVariable String type,
                         @RequestParam String id,
                         RedirectAttributes redirectAttributes) {
        MasterDefinition definition = masterDataService.definition(type);
        try {
            masterDataService.delete(definition, id);
            redirectAttributes.addFlashAttribute("success", "删除成功");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "删除失败：请先确认没有被运单或库存引用。");
        }
        return "redirect:/master/" + type;
    }
}
