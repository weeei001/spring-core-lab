package dev.jianwii.api.equipment;

import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.entity.equipment.Equipment;
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
 * equipment-borrow 的內部 API：成功直接回物件，不包 Result；失敗仍回 Result.failure 與 HTTP 狀態，
 * 由 common 的 RemoteErrorHandler 還原成 RemoteServiceException。
 *
 * <ul>
 * <li>提供端：equipment-borrow 的 EquipmentController implements 此介面。</li>
 * <li>呼叫端：{@code @ImportHttpServices(group = "equipment-borrow", types = EquipmentApi.class)}，
 * 群組名稱即 Eureka 服務名稱。前端經 web-server 的 WebEquipmentController 呼叫，由它包成 Result。</li>
 * </ul>
 */
@HttpExchange("/api/equipment")
public interface EquipmentApi {

    /** 依 id 排序，最多 100 筆。 */
    @GetExchange
    List<Equipment> getAll();

    /** 查無資料時對方回 404，呼叫端收到 RemoteServiceException。 */
    @GetExchange("/{id}")
    Equipment getById(@PathVariable("id") Long id);

    /** 依條件查詢，page 必須為 1、最多 size 筆。 */
    @PostExchange("/query")
    List<Equipment> query(@Valid @RequestBody EquipmentQueryParameter parameter);

    @PostExchange("/queryPage")
    PageResponse<Equipment> queryPage(@Valid @RequestBody EquipmentQueryParameter parameter);

    @PostExchange
    Equipment create(@Validated(ValidGroup.Create.class) @RequestBody Equipment equipment);

    /** 只更新非 null 欄位。 */
    @PatchExchange("/{id}")
    Equipment update(@PathVariable("id") Long id,
            @Validated(ValidGroup.Update.class) @RequestBody Equipment equipment);

    @DeleteExchange("/{id}")
    void delete(@PathVariable("id") Long id);
}
