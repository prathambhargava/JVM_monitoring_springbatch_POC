package com.springBatch.POC.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("invokejob")
public class TriggerJobController {

    @Autowired
    JobLauncher jobLauncher;

    @Autowired
    Job jsonProcessingJob;

    @PostMapping("/launch")
    public String handle() throws Exception {

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis()).toJobParameters();

        jobLauncher.run(jsonProcessingJob, jobParameters);

        return "Batch job has been invoked";
    }
}
