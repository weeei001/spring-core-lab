package dev.jianwii.common.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

/** 建立連線與初始化資料前檢查部署必要設定；錯誤只列出設定名稱，不輸出值。 */
public class DeployConfigurationValidator implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        ConfigurableEnvironment env = context.getEnvironment();
        if (!env.acceptsProfiles(Profiles.of("deploy"))) {
            return;
        }
        List<String> errors = new ArrayList<>();
        if (env.acceptsProfiles(Profiles.of("local"))) {
            errors.add("deploy 不可同時啟用 local");
        }
        require(env, errors, "INTERNAL_BIND_ADDRESS", "EUREKA_URL", "SERVICE_IP_ADDRESS");
        String service = env.getProperty("spring.application.name", "");
        String database = switch (service) {
            case "basic-data" -> "BASIC_DATA";
            case "project-service" -> "PROJECT";
            case "notification-service" -> "NOTIFICATION";
            case "web-server" -> "WEB_SESSION";
            default -> null;
        };
        if (database != null) {
            require(env, errors, database + "_DB_URL", database + "_DB_USERNAME", database + "_DB_PASSWORD");
            String username = env.getProperty(database + "_DB_USERNAME", "").isBlank()
                    ? ""
                    : env.getProperty("spring.datasource.username", "");
            String url = env.getProperty(database + "_DB_URL", "").isBlank()
                    ? ""
                    : env.getProperty("spring.datasource.url", "").toLowerCase(Locale.ROOT);
            if ("root".equalsIgnoreCase(username.strip())) {
                errors.add(database + "_DB_USERNAME 不可使用 root");
            }
            if (!url.startsWith("jdbc:mysql:")
                    || url.contains("createdatabaseifnotexist")
                    || url.contains("user=")
                    || url.contains("password=")) {
                errors.add(database + "_DB_URL 必須是 MySQL URL，且不可夾帶帳密或自動建庫設定");
            }
            if (!"web-server".equals(service) && !"validate".equals(env.getProperty("spring.jpa.hibernate.ddl-auto"))) {
                errors.add("deploy 的 spring.jpa.hibernate.ddl-auto 必須為 validate");
            }
        }
        if ("basic-data".equals(service)) {
            require(env, errors, "APP_UPLOAD_DIR");
            directory(env, errors, "APP_UPLOAD_DIR", true, true);
            directory(env, errors, "APP_LEGACY_UPLOAD_DIR", false, false);
        }
        if ("project-service".equals(service)) {
            require(env, errors, "APP_LEGACY_ATTACHMENT_DIR");
            directory(env, errors, "APP_LEGACY_ATTACHMENT_DIR", false, false);
        }
        if ("web-server".equals(service)) {
            if (!"never".equals(env.getProperty("spring.session.jdbc.initialize-schema"))) {
                errors.add("deploy 的 spring.session.jdbc.initialize-schema 必須為 never");
            }
            require(env, errors, "APP_FRONTEND_URL", "APP_PUBLIC_URL");
        }
        if ("notification-service".equals(service)) {
            require(
                    env,
                    errors,
                    "SMTP_HOST",
                    "SMTP_PORT",
                    "SMTP_USERNAME",
                    "SMTP_PASSWORD",
                    "SMTP_FROM",
                    "APP_FRONTEND_URL");
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("部署設定缺漏或無效：" + String.join("；", errors));
        }
    }

    private static void require(ConfigurableEnvironment env, List<String> errors, String... names) {
        for (String name : names) {
            if (env.getProperty(name, "").isBlank()) {
                errors.add(name);
            }
        }
    }

    private static void directory(
            ConfigurableEnvironment env, List<String> errors, String name, boolean writable, boolean external) {
        String value = env.getProperty(name, "");
        if (value.isBlank()) {
            return;
        }
        try {
            Path path = Path.of(value);
            if (!path.isAbsolute()
                    || !Files.isDirectory(path)
                    || !Files.isReadable(path)
                    || (writable && !Files.isWritable(path))) {
                errors.add(name + " 必須是已存在且可讀" + (writable ? "寫" : "") + "的絕對目錄");
                return;
            }
            if (external && path.toRealPath().startsWith(Path.of("").toRealPath())) {
                errors.add(name + " 必須位於程式工作目錄之外");
            }
        } catch (Exception e) {
            errors.add(name + " 目錄無效");
        }
    }
}
