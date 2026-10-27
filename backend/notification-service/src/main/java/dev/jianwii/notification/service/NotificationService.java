package dev.jianwii.notification.service;

import dev.jianwii.api.notification.NotificationQueryParameter;
import dev.jianwii.api.notification.NotificationSendRequest;
import dev.jianwii.common.exception.ForbiddenException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.notification.Notification;
import dev.jianwii.notification.dao.NotificationDAO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 站內通知的建立、查詢與已讀。建立要透過 {@link NotificationSender}（先向 basic-data 查收件人）， BaseService 的
 * create、update、delete 不對外開放。
 */
@Service
public class NotificationService extends BaseService<Notification, NotificationQueryParameter> {
    private final NotificationDAO notificationDAO;
    private final EmailOutbox emailOutbox;

    public NotificationService(NotificationDAO notificationDAO, EmailOutbox emailOutbox) {
        super(notificationDAO);
        this.notificationDAO = notificationDAO;
        this.emailOutbox = emailOutbox;
    }

    /**
     * 每位收件人建一筆通知；要寄信時，有 Email 的收件人同時寫一筆待寄信件。 兩者在同一個交易，commit 後才會寄出。
     *
     * @param emails 收件人 id → Email，沒有 Email 的不在其中
     */
    public List<Notification> createAll(NotificationSendRequest request, Map<String, String> emails) {
        List<Notification> notifications = new ArrayList<>();
        List<Notification> existing = new ArrayList<>();
        for (String recipientId : request.getRecipientIds()) {
            if (request.getDedupKey() != null) {
                Optional<Notification> duplicate =
                        notificationDAO.findByRecipientIdAndDedupKey(recipientId, request.getDedupKey());
                if (duplicate.isPresent()) {
                    existing.add(duplicate.get());
                    continue;
                }
            }
            Notification notification = new Notification();
            notification.setDedupKey(request.getDedupKey());
            notification.setRecipientId(recipientId);
            notification.setType(request.getType());
            notification.setTitle(request.getTitle());
            notification.setContent(request.getContent());
            notification.setLink(request.getLink());
            notifications.add(notification);
        }
        List<Notification> saved = notificationDAO.saveAllAndFlush(notifications);
        if (request.isEmail() && !saved.isEmpty()) {
            emailOutbox.enqueue(
                    saved.stream()
                            .filter(notification -> emails.containsKey(notification.getRecipientId()))
                            .toList(),
                    emails);
        }
        existing.addAll(saved);
        return existing;
    }

    @Transactional(readOnly = true)
    public long unreadCount(String recipientId) {
        return notificationDAO.countByRecipientIdAndReadTimeIsNull(recipientId);
    }

    /** 只能標記自己的通知；已讀的不改，已讀時間維持第一次讀的時間。 */
    public Notification markRead(Long id, String recipientId) {
        Notification notification = find(id);
        if (!notification.getRecipientId().equals(recipientId)) {
            throw new ForbiddenException("只能標記自己的通知");
        }
        if (notification.getReadTime() != null) {
            return notification;
        }
        notification.setReadTime(LocalDateTime.now());
        return notificationDAO.saveAndFlush(notification);
    }

    /**
     * 逐筆更新而不是一句 UPDATE：批次 UPDATE 會略過 JPA Auditing，transId、transTime 不會更新（見 BaseDAO）。
     */
    public int markAllRead(String recipientId) {
        List<Notification> unread = notificationDAO.findByRecipientIdAndReadTimeIsNull(recipientId);
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(notification -> notification.setReadTime(now));
        notificationDAO.saveAll(unread);
        return unread.size();
    }

    /** unread：readTime 是否為 null，條件清單表達不了 IS NULL，在這裡補上。 */
    @Override
    protected Specification<Notification> toSpecification(NotificationQueryParameter parameter) {
        Specification<Notification> specification = super.toSpecification(parameter);
        Boolean unread = parameter.getUnread();
        if (unread == null) {
            return specification;
        }
        return specification.and((root, query, builder) ->
                unread ? builder.isNull(root.get("readTime")) : builder.isNotNull(root.get("readTime")));
    }

    @Override
    protected Set<String> sortableFields() {
        return Set.of("id", "createTime", "readTime");
    }
}
