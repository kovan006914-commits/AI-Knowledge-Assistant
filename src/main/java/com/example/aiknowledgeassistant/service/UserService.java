package com.example.aiknowledgeassistant.service;


import com.example.aiknowledgeassistant.entity.User;
import com.example.aiknowledgeassistant.mapper.UserMapper;
import org.springframework.stereotype.Service;


@Service
public class UserService {


    private final UserMapper userMapper;


    public UserService(UserMapper userMapper){

        this.userMapper = userMapper;

    }


    public boolean register(User user){

        int result = userMapper.insert(user);

        return result > 0;

    }

}