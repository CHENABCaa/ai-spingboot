package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.aispingboot.entity.KnowledgeArticle;
import org.example.aispingboot.entity.KnowledgeCategory;
import org.example.aispingboot.entity.User;
import org.example.aispingboot.mapper.KnowledgeArticleMapper;
import org.example.aispingboot.mapper.KnowledgeCategoryMapper;
import org.example.aispingboot.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KnowledgeArticleService {

    @Resource
    private KnowledgeArticleMapper knowledgeArticleMapper;

    @Resource
    private KnowledgeCategoryMapper knowledgeCategoryMapper;

    @Resource
    private UserMapper userMapper;

    public Page<KnowledgeArticle> page(long currentPage, long size, String sortField, String sortDirection) {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeArticle::getStatus, 1);
        wrapper.orderByDesc(KnowledgeArticle::getPublishedAt);

        Page<KnowledgeArticle> page = knowledgeArticleMapper.selectPage(new Page<>(currentPage, size), wrapper);

        fillExtraFields(page.getRecords());
        return page;
    }

    public KnowledgeArticle getById(String id) {
        KnowledgeArticle article = knowledgeArticleMapper.selectById(id);
        if (article == null) return null;
        // 增加阅读量
        article.setReadCount((article.getReadCount() == null ? 0 : article.getReadCount()) + 1);
        knowledgeArticleMapper.updateById(article);
        fillExtraFields(List.of(article));
        return article;
    }

    private void fillExtraFields(List<KnowledgeArticle> articles) {
        if (articles.isEmpty()) return;

        // 收集所有 categoryId 和 authorId
        List<Long> categoryIds = articles.stream()
                .map(KnowledgeArticle::getCategoryId).distinct().toList();
        List<Long> authorIds = articles.stream()
                .map(KnowledgeArticle::getAuthorId).filter(id -> id != null).distinct().toList();

        Map<Long, String> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<KnowledgeCategory> cats = knowledgeCategoryMapper.selectBatchIds(categoryIds);
            categoryMap = cats.stream()
                    .collect(Collectors.toMap(KnowledgeCategory::getId, KnowledgeCategory::getCategoryName, (a, b) -> a));
        }

        Map<Long, String> authorMap = new HashMap<>();
        if (!authorIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(authorIds);
            authorMap = users.stream()
                    .collect(Collectors.toMap(User::getId, u -> u.getDisplayName() != null ? u.getDisplayName() : u.getUsername(), (a, b) -> a));
        }

        final Map<Long, String> catFinal = categoryMap;
        final Map<Long, String> authFinal = authorMap;
        articles.forEach(a -> {
            a.setCategoryName(catFinal.get(a.getCategoryId()));
            a.setAuthorName(a.getAuthorId() != null ? authFinal.get(a.getAuthorId()) : null);
        });
    }
}
