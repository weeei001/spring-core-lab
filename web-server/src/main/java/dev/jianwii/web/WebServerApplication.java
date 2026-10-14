package dev.jianwii.web;

import dev.jianwii.api.basicdata.SerialNumberApi;
import dev.jianwii.api.product.BrandApi;
import dev.jianwii.api.product.ProductApi;
import dev.jianwii.api.product.TagApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.web.service.registry.ImportHttpServices;

// common 會帶入 JPA（Entity、QueryParameter 需要），但 web-server 沒有自己的資料庫，
// 排除 DataSource 自動設定，啟動時才不會要求資料庫連線
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
// 產生各服務 XxxApi 的實作 bean，注入後呼叫方法即送出 HTTP 請求；
// group 即 Eureka 上的服務名稱，由 Spring Cloud LoadBalancer 挑選實例
@ImportHttpServices(group = "basic-data", types = SerialNumberApi.class)
@ImportHttpServices(group = "product-service", types = {ProductApi.class, BrandApi.class, TagApi.class})
public class WebServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebServerApplication.class, args);
    }
}
