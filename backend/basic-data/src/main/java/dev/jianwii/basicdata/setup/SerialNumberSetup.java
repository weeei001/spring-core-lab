package dev.jianwii.basicdata.setup;

import dev.jianwii.basicdata.dao.SerialNumberDAO;
import dev.jianwii.basicdata.service.SerialNumberService;
import dev.jianwii.entity.basicdata.ResetCycle;
import dev.jianwii.entity.basicdata.SerialNumber;
import lombok.AllArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 啟動時建立各服務需要的編號規則；已存在的不覆蓋（可能已被管理者調整過）。
 * 新服務要取號時，在這裡加一筆規則。
 */
@Component
@AllArgsConstructor
public class SerialNumberSetup implements ApplicationRunner {
    private final SerialNumberDAO serialNumberDAO;
    private final SerialNumberService serialNumberService;

    @Override
    public void run(ApplicationArguments args) {
        // 例：PRJ2610001、T26100001，每月從 1 開始
        createIfAbsent("PROJECT", "專案系統編號", "PRJ{yyMM}{000}", ResetCycle.MONTH);
        createIfAbsent("TASK", "任務系統編號", "T{yyMM}{0000}", ResetCycle.MONTH);
    }

    private void createIfAbsent(String serialKey, String name, String rule,
            ResetCycle resetCycle) {
        if (serialNumberDAO.existsBySerialKey(serialKey)) {
            return;
        }
        SerialNumber serialNumber = new SerialNumber();
        serialNumber.setSerialKey(serialKey);
        serialNumber.setName(name);
        serialNumber.setRule(rule);
        serialNumber.setResetCycle(resetCycle);
        serialNumberService.create(serialNumber);
    }
}
