package com.maram.payroll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the MARAM Confection Payroll Platform backend.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PayrollApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayrollApplication.class, args);
    }
}
