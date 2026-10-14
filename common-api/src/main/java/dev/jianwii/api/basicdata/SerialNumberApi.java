package dev.jianwii.api.basicdata;

import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.entity.basicdata.SerialNumber;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * basic-data 的系統編號 API：成功直接回物件，不包 Result；失敗仍回 Result.failure 與 HTTP 狀態，
 * 由 common 的 RemoteErrorHandler 還原成 RemoteServiceException。
 *
 * <ul>
 * <li>提供端：basic-data 的 SerialNumberController implements 此介面。</li>
 * <li>呼叫端：{@code @ImportHttpServices(group = "basic-data", types = SerialNumberApi.class)}。
 * 各服務新增資料時呼叫 {@link #next} 取號；規則維護經 web-server 的 WebSerialNumberController。</li>
 * </ul>
 */
@HttpExchange("/api/basic-data/serial-number")
public interface SerialNumberApi {

    /**
     * 依規則取下一個編號；同一規則同時取號會排隊，不會重複。
     * 號碼在 basic-data 的交易內就確定，呼叫端之後新增失敗不會退回（會跳號）。
     * 規則不存在時回 404；流水號超過 rule 的位數時回 400。
     */
    @PostExchange("/{serialKey}/next")
    String next(@PathVariable("serialKey") String serialKey);

    /** 依 id 排序，最多 100 筆。 */
    @GetExchange
    List<SerialNumber> getAll();

    @GetExchange("/{id}")
    SerialNumber getById(@PathVariable("id") Long id);

    /** 依條件查詢，page 必須為 1、最多 size 筆。 */
    @PostExchange("/query")
    List<SerialNumber> query(@Valid @RequestBody SerialNumberQueryParameter parameter);

    @PostExchange
    SerialNumber create(
            @Validated(ValidGroup.Create.class) @RequestBody SerialNumber serialNumber);

    /** 只更新非 null 欄位；serialKey 不可改，lastIndex 由取號維護、不接受修改。 */
    @PatchExchange("/{id}")
    SerialNumber update(@PathVariable("id") Long id,
            @Validated(ValidGroup.Update.class) @RequestBody SerialNumber serialNumber);

    @DeleteExchange("/{id}")
    void delete(@PathVariable("id") Long id);
}
