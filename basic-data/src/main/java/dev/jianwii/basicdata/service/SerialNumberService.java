package dev.jianwii.basicdata.service;

import dev.jianwii.api.basicdata.SerialNumberQueryParameter;
import dev.jianwii.basicdata.dao.SerialNumberDAO;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ConflictException;
import dev.jianwii.common.exception.NotFoundException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.basicdata.SerialNumber;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SerialNumberService extends BaseService<SerialNumber, SerialNumberQueryParameter> {
    private static final Pattern TOKEN = Pattern.compile("\\{([^{}]*)}");

    private final SerialNumberDAO serialNumberDAO;

    public SerialNumberService(SerialNumberDAO serialNumberDAO) {
        super(serialNumberDAO);
        this.serialNumberDAO = serialNumberDAO;
    }

    /**
     * 鎖住規則再遞增，多個服務、多個實例同時取號也不會重複。
     * 每次取號都會更新 transTime，因此以 transTime 判斷是否已進入新週期，不需要排程歸零。
     */
    @Transactional
    public String next(String serialKey) {
        SerialNumber serialNumber = serialNumberDAO
                .findWithLockBySerialKeyAndDbMark(serialKey, SerialNumber.VALID)
                .orElseThrow(() -> new NotFoundException("查無編號規則：" + serialKey));
        // 與 JPA Auditing 寫入 transTime 使用同一個時區（系統預設）
        LocalDate today = LocalDate.now();
        boolean samePeriod = serialNumber.getResetCycle()
                .samePeriod(serialNumber.getTransTime().toLocalDate(), today);
        long index = samePeriod ? serialNumber.getLastIndex() + 1 : 1;
        String result = render(serialNumber.getRule(), today, index);
        serialNumber.setLastIndex(index);
        serialNumberDAO.saveAndFlush(serialNumber);
        return result;
    }

    @Override
    protected void beforeCreate(SerialNumber request) {
        if (serialNumberDAO.existsBySerialKey(request.getSerialKey())) {
            throw new ConflictException("編號規則已存在（含已刪除）：" + request.getSerialKey());
        }
        requireValidRule(request.getRule());
        if (request.getLastIndex() == null) {
            request.setLastIndex(0L);
        }
    }

    @Override
    protected void beforeUpdate(SerialNumber entity, SerialNumber request) {
        if (request.getSerialKey() != null && !request.getSerialKey()
                .equals(entity.getSerialKey())) {
            throw new BusinessException("serialKey 建立後不可修改");
        }
        if (request.getRule() != null) {
            requireValidRule(request.getRule());
        }
        // lastIndex 只由取號維護，調小會產生重複編號
        request.setLastIndex(null);
    }

    /** 跟 tvbs 的 checkSettingRule 一樣，先試產生一次，失敗就不讓規則存進去。 */
    private void requireValidRule(String rule) {
        render(rule, LocalDate.now(), 1);
    }

    private String render(String rule, LocalDate date, long index) {
        Matcher matcher = TOKEN.matcher(rule);
        StringBuilder result = new StringBuilder();
        int sequences = 0;
        while (matcher.find()) {
            String token = matcher.group(1);
            String value;
            if (!token.isEmpty() && token.chars().allMatch(character -> character == '0')) {
                sequences++;
                String sequence = String.valueOf(index);
                if (sequence.length() > token.length()) {
                    throw new BusinessException("編號已達上限：" + rule);
                }
                value = "0".repeat(token.length() - sequence.length()) + sequence;
            } else if (token.matches("[yMd]+")) {
                value = date.format(DateTimeFormatter.ofPattern(token));
            } else {
                throw new BusinessException("rule 不支援 {" + token + "}，日期只能用 y、M、d，流水號用 0");
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        if (sequences != 1) {
            throw new BusinessException("rule 必須剛好包含一個流水號，例如 {0000}");
        }
        return result.toString();
    }
}
