package dev.jianwii.entity.basicdata;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 功能權限清單。由工程師定義、跟著程式走；管理員只能把它們組合成角色（{@link Role}），不能新增。
 *
 * <ul>
 * <li>web-server 的 Controller 以 {@code @PreAuthorize("hasAuthority('product:write')")} 檢查 code，
 * 程式只檢查功能權限，不檢查角色名稱。</li>
 * <li>新增功能時在這裡加一項，再到 Controller 標上；code 寫錯會變成所有人都沒權限（只有超級管理員能用）。</li>
 * <li>移除某一項後，角色裡留著的舊 code 會被忽略，不必清資料。</li>
 * </ul>
 */
// 轉 JSON 時輸出 code、group、description，給權限管理畫面列出可勾選的項目
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Getter
public enum Permission {

    PRODUCT_READ("product:read", "商品", "查詢商品、品牌、標籤"),
    PRODUCT_WRITE("product:write", "商品", "新增、修改、刪除商品"),
    PRODUCT_STOCK("product:stock", "商品", "調整庫存"),
    BRAND_WRITE("brand:write", "商品", "維護品牌"),
    TAG_WRITE("tag:write", "商品", "維護標籤"),
    SERIAL_NUMBER_READ("serial-number:read", "基本資料", "查詢編號規則"),
    SERIAL_NUMBER_WRITE("serial-number:write", "基本資料", "維護編號規則"),
    ROLE_MANAGE("role:manage", "系統管理", "維護角色、指派使用者角色");

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
