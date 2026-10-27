package dev.jianwii.notification.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.notification.Notification;
import java.util.List;
import java.util.Optional;

public interface NotificationDAO extends BaseDAO<Notification, Long> {

    Optional<Notification> findByRecipientIdAndDedupKey(String recipientId, String dedupKey);

    long countByRecipientIdAndReadTimeIsNull(String recipientId);

    List<Notification> findByRecipientIdAndReadTimeIsNull(String recipientId);
}
