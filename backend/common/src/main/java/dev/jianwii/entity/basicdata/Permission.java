package dev.jianwii.entity.basicdata;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * 功能權限清單。由工程師定義、跟著程式走；管理員只能把它們組合成角色（{@link Role}），不能新增。
 *
 * <ul>
 * <li>web-server 的 Controller 以 {@code @PreAuthorize("hasAuthority('task:write')")} 檢查 code，
 * 程式只檢查功能權限，不檢查角色名稱。</li>
 * <li>新增功能時在這裡加一項，再到 Controller 標上；code 寫錯會變成所有人都沒權限（只有超級管理員能用）。</li>
 * <li>移除某一項後，角色裡留著的舊 code 會被忽略，不必清資料。</li>
 * </ul>
 */
// 轉 JSON 時輸出 code、group、description，給權限管理畫面列出可勾選的項目
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Getter
public enum Permission {
    PROJECT_READ("project:read", "專案", "查詢自己參與的專案、任務、會議紀錄、留言與工時"),
    PROJECT_WRITE("project:write", "專案", "建立專案；擔任經理時維護專案、成員與標籤"),
    PROJECT_ADMIN("project:admin", "專案", "不受成員限制，可存取所有專案；能否修改看其他權限"),
    TASK_WRITE("task:write", "專案", "新增、修改、刪除任務與留言"),
    MEETING_WRITE("meeting:write", "專案", "新增會議紀錄；修改自己的紀錄，經理可修改專案內紀錄"),
    WORK_LOG_WRITE("work-log:write", "專案", "登記自己的工時"),
    LEAVE_APPLY("leave:apply", "請假", "送出請假申請，查看自己的申請與結果"),
    LEAVE_APPROVE("leave:approve", "請假", "查看並審核指定給自己的請假申請"),
    SERIAL_NUMBER_READ("serial-number:read", "基本資料", "查詢編號規則"),
    SERIAL_NUMBER_WRITE("serial-number:write", "基本資料", "維護編號規則"),
    ROLE_MANAGE("role:manage", "系統管理", "維護角色、指派使用者角色"),
    NOTIFICATION_SEND("notification:send", "系統管理", "發送公告通知給指定的使用者");

    private static final Set<String> CODES =
            Arrays.stream(values()).map(Permission::getCode).collect(Collectors.toUnmodifiableSet());

    /** 權限代碼，格式為「資源:動作」。 */
    private final String code;
    /** 畫面分組用，通常是子系統名稱。 */
    private final String group;

    private final String description;

    Permission(String code, String group, String description) {
        this.code = code;
        this.group = group;
        this.description = description;
    }

    /** 所有權限代碼；超級管理員擁有全部。 */
    public static Set<String> codes() {
        return CODES;
    }

    public static boolean exists(String code) {
        return CODES.contains(code);
    }
}
