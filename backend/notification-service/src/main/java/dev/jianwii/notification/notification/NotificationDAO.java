package dev.jianwii.notification.notification;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.notification.Notification;

import java.util.List;

public interface NotificationDAO extends BaseDAO<Notification, Long> {

    long countByRecipientIdAndReadTimeIsNullAndDbMark(String recipientId, Integer dbMark);

    List<Notification> findByRecipientIdAndReadTimeIsNullAndDbMark(String recipientId,
            Integer dbMark);
}
