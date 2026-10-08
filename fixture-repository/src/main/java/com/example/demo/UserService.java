package com.example.demo;

public class UserService {
    private UserRepository userRepository;

    public void serve() {
        userRepository.find();
    }
}
