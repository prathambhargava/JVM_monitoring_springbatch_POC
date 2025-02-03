package com.springBatch.POC.batchSkip;

import org.springframework.batch.core.SkipListener;
import com.springBatch.POC.entity.Customer;

public class CustomSkipListener implements SkipListener<Customer, Customer> {

    @Override
    public void onSkipInRead(Throwable t) {
        System.out.println("Skipped during reading: " + t.getMessage());
        t.printStackTrace();
    }

    @Override
    public void onSkipInWrite(Customer customer, Throwable t) {
        System.out.println("Skipped during writing: " + customer);
    }

    @Override
    public void onSkipInProcess(Customer customer, Throwable t) {
        System.out.println("Skipped during processing: " + customer);
    }
}

