package dev.jianwii.product;

import dev.jianwii.api.basicdata.SerialNumberApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.web.service.registry.ImportHttpServices;

@SpringBootApplication
// Entity 都在 common，只掃自己負責的
@EntityScan("dev.jianwii.entity.product")
// 新增商品時向 basic-data 取系統編號；群組名稱即 Eureka 服務名稱
@ImportHttpServices(group = "basic-data", types = SerialNumberApi.class)
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
