package com.example.aiknowledgeassistant.mapper;


import com.example.aiknowledgeassistant.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface UserMapper {


    @Insert("""
            INSERT INTO user(username,password,email)
            VALUES(#{username},#{password},#{email})
            """)
    int insert(User user);

}