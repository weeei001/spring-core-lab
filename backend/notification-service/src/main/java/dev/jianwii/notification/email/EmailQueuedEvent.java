package dev.jianwii.notification.email;

import java.util.List;

/** 交易內寫入了待寄信件；由 {@link EmailDispatcher} 在 commit 後收到並寄出。 */
public record EmailQueuedEvent(List<Long> ids) {
}
