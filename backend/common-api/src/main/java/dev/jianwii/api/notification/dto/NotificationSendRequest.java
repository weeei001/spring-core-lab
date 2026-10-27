package dev.jianwii.api.notification;

import dev.jianwii.entity.notification.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/** 發送通知：每位收件人各建一筆站內通知；email 為 true 時另寄一封信給有 Email 的收件人。 */
@Getter
@Setter
public class NotificationSendRequest {

    /** 收件人的 user id，必須是有效使用者。 */
    @NotEmpty
    @Size(max = 100)
    private Set<@NotBlank String> recipientIds;

    @NotNull
    private NotificationType type;

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String content;

    /** 前端路徑，必須以單一 / 開頭（不能是 // 或 /\ 開頭，瀏覽器會當成外部網址）。 */
    @Size(max = 500)
    @Pattern(regexp = "/(?![/\\\\]).*", message = "必須是 / 開頭的站內路徑")
    private String link;

    private boolean email;

    /** 同一收件人與此鍵只建立一筆通知；排程重試時避免重複通知及寄信。 */
    @Size(max = 100)
    @Pattern(regexp = "[A-Za-z0-9:._-]+")
    private String dedupKey;
}
