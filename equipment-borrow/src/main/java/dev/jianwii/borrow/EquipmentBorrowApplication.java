package dev.jianwii.borrow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication
// Entity 都在 common，只掃自己負責的
@EntityScan("dev.jianwii.entity.equipment")
public class EquipmentBorrowApplication {

    public static void main(String[] args) {
        SpringApplication.run(EquipmentBorrowApplication.class, args);
    }
}
