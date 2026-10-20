package dev.jianwii.api.basicdata;

import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.entity.basicdata.SerialNumber;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * basic-data 的系統編號 API（Spring HTTP Interface），提供端與呼叫端共用同一個介面：
 *
 * <ul>
 * <li>提供端：basic-data 的 SerialNumberController implements 此介面，這裡的路徑與驗證註解直接生效。</li>
 * <li>呼叫端：啟動類別加 {@code @ImportHttpServices(group = "basic-data", types = SerialNumberApi.class)}
 * 後即可注入 SerialNumberApi，Spring 會自動產生實作，呼叫方法就是送出 HTTP 請求。 各服務新增資料時呼叫 {@link #next} 取號；規則維護由前端經
 * web-server 的 WebSerialNumberController。</li>
 * </ul>
 *
 * 成功時直接回傳物件，不包 Result（包 Result 是 web-server 回前端時才做）； 失敗時對方回 Result.failure 與 HTTP 狀態，呼叫端會收到
 * RemoteServiceException。
 */
@HttpExchange("/api/basic-data/serial-number")
public interface SerialNumberApi {

    /** 不分頁查詢（例如下拉選單）：page 必須為 1，最多回傳 size 筆。 */
    @PostExchange("/query")
    List<SerialNumber> query(@Valid @RequestBody SerialNumberQueryParameter parameter);

    /** 分頁查詢，page 從 1 起算。 */
    @PostExchange("/queryPage")
    PageResponse<SerialNumber> queryPage(@Valid @RequestBody SerialNumberQueryParameter parameter);

    /** 新增；只檢查 ValidGroup.Create 與未分組的驗證規則。 */
    @PostExchange
    SerialNumber create(@Validated(ValidGroup.Create.class) @RequestBody SerialNumber serialNumber);

    /** 部分更新：只更新有給值（非 null）的欄位；serialKey 不可改，lastIndex 由取號維護、不接受修改。 */
    @PatchExchange("/{id}")
    SerialNumber update(@PathVariable("id") Long id,
            @Validated(ValidGroup.Update.class) @RequestBody SerialNumber serialNumber);

    /** 軟刪除（只標記為已刪除，資料仍在資料表）；刪除後該規則無法取號，serialKey 也不能再用來建立新規則。 */
    @DeleteExchange("/{id}")
    void delete(@PathVariable("id") Long id);

    // ---- 以下為編號專屬的業務 API ----

    /**
     * 依規則取下一個編號；同一規則同時取號會排隊，不會重複。 號碼在 basic-data 那邊就已確定並存檔，呼叫端之後新增失敗也不會退回（會跳號）。 規則不存在時回 404；流水號超過
     * rule 的位數時回 400。
     */
    @PostExchange("/{serialKey}/next")
    String next(@PathVariable("serialKey") String serialKey);
}
