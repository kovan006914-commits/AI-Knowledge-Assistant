package com.example.aiknowledgeassistant;


import com.example.aiknowledgeassistant.entity.User;
import com.example.aiknowledgeassistant.service.UserService;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/user")
public class UserController {


    private final UserService userService;


    public UserController(UserService userService){

        this.userService = userService;

    }


    @PostMapping("/register")
    public String register(@RequestBody User user){

        boolean success = userService.register(user);

        if(success){

            return "注册成功";

        }else{

            return "注册失败";

        }

    }

}