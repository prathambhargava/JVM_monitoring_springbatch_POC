package com.springBatch.POC.Config;

import com.springBatch.POC.batchSkip.CustomSkipListener;
import com.springBatch.POC.batchSkip.skipPolicy;
import com.springBatch.POC.entity.Customer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.builder.JsonItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;
import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Objects;
import java.util.UUID;

@Configuration
@Slf4j
public class BatchConfig {

    @Autowired
    private DataSource dataSource;

    String errorCode = null;
    String errorMessage = null;
    private int batchCounter = 1;

    @Bean
    public ItemReader<Customer> jsonItemReader() {
        JacksonJsonObjectReader<Customer> jsonObjectReader = new JacksonJsonObjectReader<>(Customer.class);

        return new JsonItemReaderBuilder<Customer>()
                .jsonObjectReader(jsonObjectReader)
                .resource(new ClassPathResource("sample_data.json"))
                .name("customerJsonItemReader")
                .build();
    }




    @Bean
    public Step jsonProcessingStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("jsonProcessingStep", jobRepository)
                .<Customer, Customer>chunk(10, transactionManager)
                .reader(jsonItemReader())
                .processor(jsonItemProcessor())
                .writer(jsonItemWriter(dataSource))
                .faultTolerant()
                .skipPolicy(new skipPolicy())
                .listener(new CustomSkipListener())
                .build();
    }

    @Bean
    public Job jsonProcessingJob(JobRepository jobRepository, Step jsonProcessingStep) {
        return new JobBuilder("jsonProcessingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(jsonProcessingStep)
                .build();
    }

    @Bean
    public ItemProcessor<Customer, Customer> jsonItemProcessor() {
        return customer -> {
            boolean hasIssues = false;
            StringBuilder errorMessages = new StringBuilder();

            for (Field field : Customer.class.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object value = field.get(customer);
                    if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                        hasIssues = true;
                        errorMessages.append("Missing field: ").append(field.getName()).append("; ");
                    } else if (value instanceof String) {

                        if (!isValidString((String) value)) {
                            hasIssues = true;
                            errorMessages.append("Corrupted field: ").append(field.getName()).append("; ");
                        }
                    } else if (!isFieldAligned(field, value)) {

                        hasIssues = true;
                        errorMessages.append("Misaligned field: ").append(field.getName()).append("; ");
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    hasIssues = true;
                    errorMessages.append("Error accessing field: ").append(field.getName()).append("; ");
                }
            }

            String randString = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String uuid = randString + "-" + batchCounter;
            batchCounter++;
            customer.setUuid(uuid);

            if (hasIssues) {
                customer.setStatus("failed");
                System.out.println("Validation failed for customer with UUID"+ uuid + "error : "+ errorMessages);
            } else {
                customer.setStatus("processed");
            }

            System.out.println("Processing customer: " + customer.getEmail() + " " + customer.getFirstName() + " " + customer.getLastName() + " " + customer.getStatus() + " " + customer.getUuid());
            return customer;
        };
    }

    private boolean isValidString(String value) {
        String pattern = "^[a-zA-Z0-9@.\\s]+$";

        boolean isUtf8 = value.equals(new String(value.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        return value.matches(pattern) && isUtf8;
    }

    private boolean isFieldAligned(Field field, Object value) {
        if (value instanceof String) {
            String stringValue = (String) value;

            if (field.getName().equalsIgnoreCase("email")) {
                String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
                return stringValue.matches(emailPattern);
            }

            }

        return true;
    }



    @Bean
    public ItemWriter<Customer> jsonItemWriter(DataSource dataSource) {
        return customers -> {
            try (Connection connection = dataSource.getConnection()) {
                String insertCustomerSql = "INSERT INTO customers_information (firstname, secondname, email, status) VALUES (?, ?, ?, ?)";
                String insertItemInfoSql = "INSERT INTO item_info (uuid, customer_id) VALUES (?, ?)";
                String insertFailedBatches = "INSERT INTO failedbatches (uuid,customer_id,status) VALUES (?,?,?)";

                for (Customer customer : customers) {
                    try (PreparedStatement customerStmt = connection.prepareStatement(insertCustomerSql, Statement.RETURN_GENERATED_KEYS)) {
                        customerStmt.setString(1, customer.getFirstName());
                        customerStmt.setString(2, customer.getLastName());
                        customerStmt.setString(3, customer.getEmail());
                        customerStmt.setString(4, customer.getStatus());

                        customerStmt.executeUpdate();

                        ResultSet generatedKeys = customerStmt.getGeneratedKeys();
                        if (generatedKeys.next()) {
                            long customerId = generatedKeys.getLong(1);

                            String updatedStatus = customer.getStatus();



                            try (PreparedStatement itemInfoStmt = connection.prepareStatement(insertItemInfoSql)) {
                                itemInfoStmt.setString(1, customer.getUuid());
                                itemInfoStmt.setLong(2, customerId);
                                itemInfoStmt.executeUpdate();
                            }
                            if (Objects.equals(updatedStatus, "failed")) {
                                try (PreparedStatement failedStmt = connection.prepareStatement(insertFailedBatches)) {
                                    failedStmt.setString(1, customer.getUuid());
                                    failedStmt.setLong(2, customerId);
                                    failedStmt.setString(3, customer.getStatus());
                                    failedStmt.executeUpdate();
                                }
                            }
                        }
                    }
                }
            };
        };
    }
}

