package com.springBatch.POC.entity;

import jakarta.persistence.*;


@Entity
@Table(name = "failedbatches")
public class failedBatches {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "response_id")
    private String id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "customer_id")
    private String customer_id;

    @Column(name = "status")
    public static String status;

    public failedBatches(){
        this.status = status;
        this.id=id;
        this.customer_id= customer_id;
        this.uuid = uuid;

    }

    public String getStatus() {
    return status;
    }

    public void setStatus(String status){
        failedBatches.status = status;
    }

    }
