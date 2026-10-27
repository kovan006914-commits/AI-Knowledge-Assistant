package com.example.aiknowledgeassistant.mapper;

import com.example.aiknowledgeassistant.entity.KnowledgeItem;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface KnowledgeMapper {
    @Select("""
        SELECT id, owner_id, title, content, tags, created_at, updated_at FROM knowledge_item
        WHERE owner_id = #{ownerId}
          AND (#{query} = '' OR LOWER(title) LIKE #{pattern} ESCAPE '!'
               OR LOWER(content) LIKE #{pattern} ESCAPE '!' OR LOWER(tags) LIKE #{pattern} ESCAPE '!')
        ORDER BY updated_at DESC, id DESC LIMIT #{limit} OFFSET #{offset}
        """)
    List<KnowledgeItem> list(@Param("ownerId") long ownerId, @Param("query") String query,
            @Param("pattern") String pattern, @Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT COUNT(*) FROM knowledge_item WHERE owner_id = #{ownerId}
          AND (#{query} = '' OR LOWER(title) LIKE #{pattern} ESCAPE '!'
               OR LOWER(content) LIKE #{pattern} ESCAPE '!' OR LOWER(tags) LIKE #{pattern} ESCAPE '!')
        """)
    long count(@Param("ownerId") long ownerId, @Param("query") String query, @Param("pattern") String pattern);

    @Select("SELECT * FROM knowledge_item WHERE id = #{id} AND owner_id = #{ownerId}")
    KnowledgeItem find(@Param("id") long id, @Param("ownerId") long ownerId);

    @Insert("""
        INSERT INTO knowledge_item(owner_id,title,content,tags,created_at,updated_at)
        VALUES(#{ownerId},#{title},#{content},#{tags},CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeItem item);

    @Update("""
        UPDATE knowledge_item SET title=#{title}, content=#{content}, tags=#{tags}, updated_at=CURRENT_TIMESTAMP
        WHERE id=#{id} AND owner_id=#{ownerId}
        """)
    int update(KnowledgeItem item);

    @Delete("DELETE FROM knowledge_item WHERE id=#{id} AND owner_id=#{ownerId}")
    int delete(@Param("id") long id, @Param("ownerId") long ownerId);
}
