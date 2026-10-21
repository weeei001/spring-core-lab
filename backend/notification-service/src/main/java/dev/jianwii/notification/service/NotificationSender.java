package dev.jianwii.notification.service;

import dev.jianwii.api.basicdata.UserApi;
import dev.jianwii.api.basicdata.UserQueryParameter;
import dev.jianwii.api.notification.NotificationSendRequest;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.entity.basicdata.User;
import dev.jianwii.entity.notification.Notification;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 發送通知：先向 basic-data 確認收件人並取得 Email，再交給 {@link NotificationService} 寫入。 獨立成元件（不在 Service
 * 的交易內），呼叫其他服務時才不會一直占著資料庫連線。
 */
@Component
@AllArgsConstructor
public class NotificationSender {
    private final UserApi userApi;
    private final NotificationService notificationService;

    public List<Notification> send(NotificationSendRequest request) {
        Map<String, String> emails = findUsers(request.getRecipientIds()).values().stream()
                .filter(user -> StringUtils.hasText(user.getEmail()))
                .collect(Collectors.toMap(user -> String.valueOf(user.getId()), User::getEmail));
        return notificationService.createAll(request, emails);
    }

    /** 收件人都必須是有效使用者（已停用的查不到），否則列出不存在的 id。 */
    private Map<String, User> findUsers(Set<String> ids) {
        // user id 是數字；system 等非數字的 id 送到 basic-data 會回 400，先在這裡擋
        List<String> invalid = ids.stream()
                .filter(id -> !id.matches("[1-9]\\d{0,17}"))
                .sorted()
                .toList();
        if (!invalid.isEmpty()) {
            throw new BusinessException("收件人不存在：" + invalid);
        }
        UserQueryParameter parameter = new UserQueryParameter();
        parameter.setIds(new ArrayList<>(ids));
        parameter.setSize(100);
        Map<String, User> users = userApi.query(parameter).stream()
                .collect(Collectors.toMap(user -> String.valueOf(user.getId()), Function.identity()));
        List<String> missing =
                ids.stream().filter(id -> !users.containsKey(id)).sorted().toList();
        if (!missing.isEmpty()) {
            throw new BusinessException("收件人不存在：" + missing);
        }
        return users;
    }
}
