package com.example.aiknowledgeassistant.service;

import com.example.aiknowledgeassistant.dto.KnowledgeRequest;
import com.example.aiknowledgeassistant.entity.KnowledgeItem;
import com.example.aiknowledgeassistant.exception.ApiProblem;
import com.example.aiknowledgeassistant.mapper.KnowledgeMapper;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeService {
    private final KnowledgeMapper mapper;
    public KnowledgeService(KnowledgeMapper mapper) { this.mapper = mapper; }
    public record Page(List<KnowledgeItem> items, long total, int page, int size) {}

    public Page list(long ownerId, String query, int page, int size) {
        if (page < 1 || page > 100000 || size < 1 || size > 100) {
            throw new IllegalArgumentException("分页参数不正确");
        }
        String q = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        if (q.length() > 100) throw new IllegalArgumentException("搜索词不能超过100个字符");
        String pattern = "%" + q.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return new Page(mapper.list(ownerId, q, pattern, size, (page - 1) * size),
                mapper.count(ownerId, q, pattern), page, size);
    }

    public KnowledgeItem get(long ownerId, long id) {
        KnowledgeItem item = mapper.find(id, ownerId);
        if (item == null) throw new ApiProblem(404, "知识条目不存在");
        return item;
    }

    private KnowledgeItem validated(long ownerId, KnowledgeRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()
                || request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("标题和正文不能为空");
        }
        String title = request.title().strip(), content = request.content().strip();
        String tags = request.tags() == null ? "" : request.tags().strip();
        if (title.length() > 120 || content.length() > 20000 || tags.length() > 200) {
            throw new IllegalArgumentException("标题最多120字、正文最多20000字、标签最多200字");
        }
        KnowledgeItem item = new KnowledgeItem();
        item.setOwnerId(ownerId); item.setTitle(title); item.setContent(content); item.setTags(tags);
        return item;
    }

    public KnowledgeItem create(long ownerId, KnowledgeRequest request) {
        KnowledgeItem item = validated(ownerId, request);
        if (mapper.insert(item) != 1 || item.getId() == null) throw new IllegalStateException("新增失败");
        return get(ownerId, item.getId());
    }

    public KnowledgeItem update(long ownerId, long id, KnowledgeRequest request) {
        get(ownerId, id); // Same 404 for missing items and another user's items.
        KnowledgeItem item = validated(ownerId, request);
        item.setId(id);
        mapper.update(item);
        return get(ownerId, id);
    }

    public void delete(long ownerId, long id) {
        if (mapper.delete(id, ownerId) != 1) throw new ApiProblem(404, "知识条目不存在");
    }

    public List<KnowledgeItem> recentForSearch(long ownerId) {
        return mapper.list(ownerId, "", "%", 200, 0);
    }
}
