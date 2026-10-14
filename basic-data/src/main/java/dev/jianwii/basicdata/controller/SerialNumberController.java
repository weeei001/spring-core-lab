package dev.jianwii.basicdata.controller;

import dev.jianwii.api.basicdata.SerialNumberApi;
import dev.jianwii.api.basicdata.SerialNumberQueryParameter;
import dev.jianwii.basicdata.service.SerialNumberService;
import dev.jianwii.entity.basicdata.SerialNumber;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 路徑、參數與驗證都定義在 {@link SerialNumberApi}，這裡只實作。
 */
@RestController
@AllArgsConstructor
public class SerialNumberController implements SerialNumberApi {
    private final SerialNumberService serialNumberService;

    @Override
    public String next(String serialKey) {
        return serialNumberService.next(serialKey);
    }

    @Override
    public List<SerialNumber> getAll() {
        return serialNumberService.getAll();
    }

    @Override
    public SerialNumber getById(Long id) {
        return serialNumberService.getById(id);
    }

    @Override
    public List<SerialNumber> query(SerialNumberQueryParameter parameter) {
        return serialNumberService.query(parameter);
    }

    @Override
    public SerialNumber create(SerialNumber serialNumber) {
        return serialNumberService.create(serialNumber);
    }

    @Override
    public SerialNumber update(Long id, SerialNumber serialNumber) {
        return serialNumberService.update(id, serialNumber);
    }

    @Override
    public void delete(Long id) {
        serialNumberService.deleteById(id);
    }
}
