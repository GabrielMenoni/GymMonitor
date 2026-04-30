package com.example.userservice.repository;

import org.springframework.stereotype.Repository;

@Repository
public class UserServiceRepository {

    public String getStatus() {
        return "USER_SERVICE_OK";
    }
}
