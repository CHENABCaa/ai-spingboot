package org.example.aispingboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.entity.KnowledgeArticle;
import org.example.aispingboot.service.KnowledgeArticleService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge/article")
public class KnowledgeArticleController {

    @Resource
    private KnowledgeArticleService knowledgeArticleService;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") long currentPage,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortDirection) {
        Page<KnowledgeArticle> page = knowledgeArticleService.page(currentPage, size, sortField, sortDirection);
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        return Result.ok(data);
    }

    @GetMapping("/{id}")
    public Result<KnowledgeArticle> detail(@PathVariable String id) {
        KnowledgeArticle article = knowledgeArticleService.getById(id);
        if (article == null) {
            return Result.error("文章不存在");
        }
        return Result.ok(article);
    }
}
