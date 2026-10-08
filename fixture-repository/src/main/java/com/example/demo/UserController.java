package com.example.demo;

public class UserController {
    private UserRepository userRepository;
    private UserService userService;

    public void getUser() {
        userRepository.find();
        userService.serve();
    }
}
