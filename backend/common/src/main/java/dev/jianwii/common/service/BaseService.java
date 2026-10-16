package dev.jianwii.common.service;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.common.dao.QuerySpecifications;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.NotFoundException;
import dev.jianwii.common.query.QueryParameter;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.beans.PropertyDescriptor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 共用 CRUD：query、queryPage、create、update（部分更新）、delete（軟刪除），輸入輸出直接用 Entity，
 * 查詢條件用 {@link QueryParameter}。已軟刪除的資料（dbMark = INVALID）查不到，也不能修改或再刪除。
 *
 * <p>
 * 用法：XxxService 繼承此類別並加 @Service，建構子把 DAO 傳給 super。
 * 需要額外檢查時覆寫 before* 方法（丟例外即中止，交易會 rollback）；可排序欄位覆寫 {@link #sortableFields()}；
 * 有 LAZY 關聯要帶回前端時覆寫 {@link #initialize}；其他業務方法直接寫在 XxxService。
 *
 * @param <E>
 *            Entity
 * @param <Q>
 *            查詢參數
 */
// 所有 public 方法都在交易內執行；查詢方法另標 readOnly
@Transactional
public abstract class BaseService<E extends BaseEntity, Q extends QueryParameter<E>> {

    /** id、軟刪除標記與審計欄位由系統維護，更新時一律不從 request 複製。 */
    private static final Set<String> MANAGED_PROPERTIES =
            Set.of("id", "dbMark", "createId", "createTime", "transId", "transTime");

    protected final BaseDAO<E, Long> dao;

    protected BaseService(BaseDAO<E, Long> dao) {
        this.dao = dao;
    }

    /** 不分頁查詢（例如下拉選單），最多回傳 size 筆；page 必須為 1，要翻頁請用 queryPage。 */
    @Transactional(readOnly = true)
    public List<E> query(Q parameter) {
        if (parameter.getPage() != 1) {
            throw new BusinessException("query 只提供有限選單，page 必須為 1");
        }
        return queryPage(parameter).getItems();
    }

    /** 分頁查詢，自動排除已軟刪除的資料。 */
    @Transactional(readOnly = true)
    public PageResponse<E> queryPage(Q parameter) {
        beforeList();
        Specification<E> valid =
                (root, query, builder) -> builder.equal(root.get("dbMark"), BaseEntity.VALID);
        Page<E> page = dao.findAll(valid.and(toSpecification(parameter)),
                PageResponse.pageable(parameter, sortableFields()));
        page.forEach(this::initialize);
        return new PageResponse<>(page);
    }

    /** 新增；id 與 dbMark 由系統設定。 */
    public E create(E request) {
        // 清掉前端送來的 id：有 id 時 save 會變成更新既有資料
        request.setId(null);
        request.setDbMark(BaseEntity.VALID);
        beforeCreate(request);
        E saved = dao.saveAndFlush(request);
        initialize(saved);
        return saved;
    }

    /** 部分更新：只複製 request 中非 null 的欄位，見 {@link #applyUpdate}。 */
    public E update(Long id, E request) {
        E entity = find(id);
        beforeUpdate(entity, request);
        applyUpdate(request, entity);
        E saved = dao.saveAndFlush(entity);
        initialize(saved);
        return saved;
    }

    /** 軟刪除：只把 dbMark 改為 INVALID，資料仍在資料表中。 */
    public void delete(Long id) {
        E entity = find(id);
        beforeDelete(entity);
        entity.setDbMark(BaseEntity.INVALID);
        dao.saveAndFlush(entity);
    }

    /** 取得有效資料；不存在或已軟刪除時丟 {@link NotFoundException}。 */
    protected E find(Long id) {
        return dao.findById(id).filter(entity -> entity.getDbMark() == BaseEntity.VALID)
                .orElseThrow(() -> new NotFoundException("查無資料，id：" + id));
    }

    /**
     * 把 request 中非 null 的欄位複製到 entity（PATCH 語意），略過 id、dbMark 與審計欄位。
     * 因此無法透過 update 把欄位設為 null；需要時覆寫此方法。
     */
    protected void applyUpdate(E request, E entity) {
        BeanWrapper source = new BeanWrapperImpl(request);
        String[] ignored = Stream.of(source.getPropertyDescriptors()).map(PropertyDescriptor::getName)
                .filter(name -> MANAGED_PROPERTIES.contains(name) || !source.isReadableProperty(
                        name) || source.getPropertyValue(name) == null)
                .toArray(String[]::new);
        BeanUtils.copyProperties(request, entity, ignored);
    }

    /**
     * 查詢條件，預設由 {@link QueryParameter#toQueryConditions} 轉換；軟刪除的過濾由 queryPage 另外加上，這裡不必處理。
     * 需要子查詢、join 等條件清單表達不了的情況時覆寫，以 {@code super.toSpecification(parameter).and(...)} 補上。
     */
    protected Specification<E> toSpecification(Q parameter) {
        return QuerySpecifications.of(parameter.toQueryConditions());
    }

    /** queryPage 允許的排序欄位；未指定排序時固定以 id 遞增。 */
    protected Set<String> sortableFields() {
        return Set.of("id", "createTime", "transTime");
    }

    /**
     * 回傳前載入要帶給前端的 LAZY 關聯，例如 {@code Hibernate.initialize(task.getPredecessorIds())}。
     * 交易結束後就不能再查資料庫，轉 JSON 時讀到沒載入的關聯會出錯，所以必須在 Service 內載入。
     * query、queryPage、create、update 回傳前都會呼叫；XxxService 自己的方法要自行呼叫。
     */
    protected void initialize(E entity) {
    }

    /**
     * 只改了 @ElementCollection（中間表，例如任務的前置任務、角色權限）時，主表沒有欄位變動，
     * JPA 不會 UPDATE 主表，transId、transTime 也就不會更新。改一下 transTime 讓主表一起 UPDATE，
     * JPA Auditing 會再填入正確的修改者與時間。
     */
    protected void touch(E entity) {
        entity.setTransTime(LocalDateTime.now());
    }

    /** 新增前的檢查或補值，例如檢查代碼重複、填入系統編號。 */
    protected void beforeCreate(E request) {
    }

    /**
     * 更新前的檢查。entity 是資料庫中的現值，request 是前端送來的新值；
     * 不允許修改的欄位可在這裡設為 null，就不會被複製。
     */
    protected void beforeUpdate(E entity, E request) {
    }

    /** 刪除前的檢查，例如仍有關聯資料時禁止刪除。 */
    protected void beforeDelete(E entity) {
    }

    /** 查詢前的檢查，例如權限。 */
    protected void beforeList() {
    }
}
