package dev.jianwii.web;

import dev.jianwii.api.basicdata.SerialNumberApi;
import dev.jianwii.api.product.ProductApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.web.service.registry.ImportHttpServices;

// common 會帶入 JPA（Entity、QueryParameter 需要），但 web-server 沒有自己的資料庫
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
// 群組名稱即 Eureka 服務名稱，由 Spring Cloud LoadBalancer 挑選實例
@ImportHttpServices(group = "basic-data", types = SerialNumberApi.class)
@ImportHttpServices(group = "product-service", types = ProductApi.class)
public class WebServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebServerApplication.class, args);
    }
}
