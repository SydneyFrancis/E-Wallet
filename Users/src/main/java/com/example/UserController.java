package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    @Autowired
    UserService userService;


    @PostMapping("/createUser")
    public void createUser(@RequestBody() UserRequestDto userRequestDto){
        userService.createUser(userRequestDto);
    }

    @GetMapping("/getUser")
    public UserEntity getUserByName(@RequestParam("username")String UserName) throws UserNotFoundException {
       return userService.getUser(UserName);
    }
}
