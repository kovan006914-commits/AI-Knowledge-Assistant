package com.example.aiknowledgeassistant.mapper;


import com.example.aiknowledgeassistant.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


@Mapper
public interface UserMapper {


    @Insert("""
            INSERT INTO user(username,password,email)
            VALUES(#{username},#{password},#{email})
            """)
    int insert(User user);

    @Select("SELECT id, username, password, email FROM user WHERE username = #{username}")
    User findByUsername(@Param("username") String username);

}