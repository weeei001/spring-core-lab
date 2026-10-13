package dev.jianwii.borrow;

import dev.jianwii.api.user.UserApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.web.service.registry.ImportHttpServices;

@SpringBootApplication
// Entity 都在 common，只掃自己負責的
@EntityScan("dev.jianwii.entity.equipment")
// 群組名稱即 Eureka 服務名稱，由 Spring Cloud LoadBalancer 挑選實例
@ImportHttpServices(group = "user-service", types = UserApi.class)
public class EquipmentBorrowApplication {

    public static void main(String[] args) {
        SpringApplication.run(EquipmentBorrowApplication.class, args);
    }
}
