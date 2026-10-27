package dev.jianwii.notification.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.notification.EmailMessage;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailMessageDAO extends BaseDAO<EmailMessage, Long> {

    /**
     * 排程要重新寄送的信：
     * <ul>
     * <li>PENDING 超過 pendingBefore 還沒寄：commit 後的非同步寄送沒執行到（例如服務剛好重啟）。</li>
     * <li>FAILED 且還沒用完次數：上次寄送失敗，間隔一段時間再試。</li>
     * <li>SENDING 超過 sendingBefore：寄到一半服務掛掉，狀態停在寄送中。</li>
     * </ul>
     */
    @Query("""
            select e.id from EmailMessage e
            where (e.status = dev.jianwii.entity.notification.EmailStatus.PENDING
                    and e.createTime < :pendingBefore)
                or (e.status = dev.jianwii.entity.notification.EmailStatus.FAILED
                    and e.attempts < :maxAttempts and e.transTime < :failedBefore)
                or (e.status = dev.jianwii.entity.notification.EmailStatus.SENDING
                    and e.transTime < :sendingBefore)
            order by e.id""")
    List<Long> findRetryableIds(
            @Param("maxAttempts") int maxAttempts,
            @Param("pendingBefore") LocalDateTime pendingBefore,
            @Param("failedBefore") LocalDateTime failedBefore,
            @Param("sendingBefore") LocalDateTime sendingBefore,
            Pageable pageable);
}
