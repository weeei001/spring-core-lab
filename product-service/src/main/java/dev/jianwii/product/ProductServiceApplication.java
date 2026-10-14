package dev.jianwii.product;

import dev.jianwii.api.basicdata.SerialNumberApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.web.service.registry.ImportHttpServices;

@SpringBootApplication
// Entity 都放在 common，這裡只掃自己負責的 package，才不會連其他服務的資料表也建出來
@EntityScan("dev.jianwii.entity.product")
// 產生 SerialNumberApi 的實作 bean，新增商品時注入它向 basic-data 取系統編號；
// group 即 Eureka 上的服務名稱，由 Spring Cloud LoadBalancer 找到實際位址
@ImportHttpServices(group = "basic-data", types = SerialNumberApi.class)
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
