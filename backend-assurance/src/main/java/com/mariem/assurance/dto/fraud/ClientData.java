package com.mariem.assurance.dto.fraud;

import lombok.Data;

@Data
public class ClientData {
    private String firstName;
    private String lastName;
    private Integer age;

    private String address;
    private String email;
    private String phone;

    public ClientData() {}

    public ClientData(String firstName, String lastName, int age, String address, String email, String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.address = address;
        this.email = email;
        this.phone = phone;
    }
}
