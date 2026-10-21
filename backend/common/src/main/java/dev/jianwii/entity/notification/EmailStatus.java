package dev.jianwii.entity.notification;

/** 寄信狀態：PENDING → SENDING → SENT；失敗為 FAILED，未超過重試次數會再回到 SENDING。 */
public enum EmailStatus {
    /** 等待寄出 */
    PENDING,
    /** 已被某個執行緒領走，正在寄 */
    SENDING,
    SENT,
    FAILED
}
