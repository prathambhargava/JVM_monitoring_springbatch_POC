package com.springBatch.POC.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name="customers_information")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Customer {

    @Id
    @Column(name = "customer_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId;
    @Column(name = "firstname")
    private String firstName;

    @Column(name = "secondname")
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "status")
    private static  String status;

    @Column(name = "uuid")
    private static String uuid;

    public Customer() {
    }


    public void setStatus(String status) {
        Customer.status = status;
    }

    public void setUuid(String uuid) {
        Customer.uuid = uuid;
    }

    public String getStatus() {
        return status;
    }

    public String getUuid() {
        return uuid;
    }

    public Customer(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public Long getId() {
        return customerId;
    }

    public void setId(Long id) {
        this.customerId = id;
    }

    public  String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public  String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String  getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

