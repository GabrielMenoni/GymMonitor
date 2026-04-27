package com.example.accesscontrol.repository;

import org.springframework.stereotype.Repository;

@Repository
public class AccessControlRepository {

    public String getStatus() {
        return "ACCESS_CONTROL_OK";
    }
}
