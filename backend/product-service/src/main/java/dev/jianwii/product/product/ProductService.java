package dev.jianwii.product.product;

import dev.jianwii.api.basicdata.SerialNumberApi;
import dev.jianwii.api.product.ProductQueryParameter;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ConflictException;
import dev.jianwii.common.exception.NotFoundException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.product.Brand;
import dev.jianwii.entity.product.Product;
import dev.jianwii.entity.product.Tag;
import dev.jianwii.product.brand.BrandDAO;
import dev.jianwii.product.tag.TagDAO;
import jakarta.persistence.criteria.JoinType;
import org.hibernate.Hibernate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 商品的 CRUD（繼承 BaseService）、批次查詢與庫存調整；新增、修改時檢查關聯的品牌與標籤存在。 */
@Service
public class ProductService extends BaseService<Product, ProductQueryParameter> {
    private static final int MAX_BATCH = 100;
    /** basic-data 的編號規則代碼，規則由 basic-data 的 SerialNumberSetup 建立。 */
    private static final String SERIAL_KEY = "PRODUCT";

    private final ProductDAO productDAO;
    private final BrandDAO brandDAO;
    private final TagDAO tagDAO;
    private final SerialNumberApi serialNumberApi;

    public ProductService(ProductDAO productDAO, BrandDAO brandDAO, TagDAO tagDAO,
            SerialNumberApi serialNumberApi) {
        super(productDAO);
        this.productDAO = productDAO;
        this.brandDAO = brandDAO;
        this.tagDAO = tagDAO;
        this.serialNumberApi = serialNumberApi;
    }

    /** 批次查詢；不存在或已刪除的 id 直接略過，回傳順序不保證與 ids 相同。 */
    @Transactional(readOnly = true)
    public List<Product> getByIds(List<Long> ids) {
        if (ids.size() > MAX_BATCH) {
            throw new BusinessException("一次最多查詢 " + MAX_BATCH + " 筆");
        }
        List<Long> distinctIds = ids.stream().distinct().toList();
        Specification<Product> byIds = (root, query, builder) -> builder.and(
                root.get("id").in(distinctIds), builder.equal(root.get("dbMark"), Product.VALID));
        List<Product> products = productDAO.findAll(byIds.and(fetchBrand()));
        products.forEach(this::initialize);
        return products;
    }

    /** 鎖住該筆商品再計算，同時出庫也不會超賣。 */
    @Transactional
    public Product adjustStock(Long id, int quantity) {
        Product product = productDAO.findWithLockByIdAndDbMark(id, Product.VALID)
                .orElseThrow(() -> new NotFoundException("查無資料，id：" + id));
        long stock = (long) product.getStock() + quantity;
        if (stock < 0) {
            throw new BusinessException("庫存不足，目前庫存：" + product.getStock());
        }
        if (stock > Integer.MAX_VALUE) {
            throw new BusinessException("庫存超出上限");
        }
        product.setStock((int) stock);
        Product saved = productDAO.saveAndFlush(product);
        initialize(saved);
        return saved;
    }

    @Override
    protected void beforeCreate(Product request) {
        requireUniqueSku(request.getSku());
        // 前端送來的 brand 不採用，換成資料庫中的品牌，回傳時才帶得出來
        request.setBrand(requireBrand(request.getBrandCode()));
        requireTags(request.getTagIds());
        if (request.getTagIds() == null) {
            request.setTagIds(new HashSet<>());
        }
        // 號碼取走就不會退回，所以檢查都通過才取號，減少跳號
        request.setCode(serialNumberApi.next(SERIAL_KEY));
    }

    @Override
    protected void beforeUpdate(Product entity, Product request) {
        // 系統編號不可修改
        request.setCode(null);
        if (request.getStock() != null) {
            throw new BusinessException("庫存請用調整庫存 API 修改");
        }
        if (request.getSku() != null && !request.getSku().equals(entity.getSku())) {
            requireUniqueSku(request.getSku());
        }
        // 前端送來的 brand 不採用，以 brandCode 為準
        request.setBrand(null);
        if (request.getBrandCode() != null && !request.getBrandCode().equals(entity.getBrandCode())) {
            // brandCode 改了，brand 也換成新品牌，回傳時才會一致
            entity.setBrand(requireBrand(request.getBrandCode()));
        }
        requireTags(request.getTagIds());
        if (request.getTagIds() != null) {
            // 只改標籤時主表不會 UPDATE，強制更新修改者與時間
            touch(entity);
        }
    }

    /** 多對多條件：tagIds 是集合，QueryParameter 的條件清單表達不了，在這裡補上。 */
    @Override
    protected Specification<Product> toSpecification(ProductQueryParameter parameter) {
        Specification<Product> specification = super.toSpecification(parameter).and(fetchBrand());
        Long tagId = parameter.getTagId();
        if (tagId == null) {
            return specification;
        }
        // tagId MEMBER OF tagIds：產生子查詢而非 join，同一筆商品不會重複出現
        return specification.and((root, query, builder) ->
                builder.isMember(tagId, root.<Set<Long>>get("tagIds")));
    }

    /** 回傳前載入 LAZY 關聯；列表的品牌已由 fetchBrand 一起查出，這裡不會再查。 */
    @Override
    protected void initialize(Product product) {
        Hibernate.initialize(product.getBrand());
        Hibernate.initialize(product.getTagIds());
    }

    /**
     * 查商品時用 LEFT JOIN FETCH 一起查出品牌，一條 SQL 完成。
     * 不這樣做的話，每筆商品會各自再查一次品牌（N+1）。
     */
    private static Specification<Product> fetchBrand() {
        return (root, query, builder) -> {
            // 分頁時 Spring Data 另外會跑一條 count 查詢，count 不能 fetch
            if (query.getResultType() != Long.class) {
                root.fetch("brand", JoinType.LEFT);
            }
            return builder.conjunction();
        };
    }

    /** 多對一：brandCode 必須是有效品牌，回傳該品牌；null 表示不設定。 */
    private Brand requireBrand(String brandCode) {
        if (brandCode == null) {
            return null;
        }
        return brandDAO.findByCodeAndDbMark(brandCode, Brand.VALID)
                .orElseThrow(() -> new BusinessException("品牌不存在：" + brandCode));
    }

    /** 多對多：tagIds 必須都是有效標籤，否則列出不存在的 id。 */
    private void requireTags(Set<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        Set<Long> found = tagDAO.findAllByIdInAndDbMark(tagIds, Tag.VALID).stream()
                .map(Tag::getId).collect(Collectors.toSet());
        List<Long> missing = tagIds.stream().filter(id -> !found.contains(id)).sorted().toList();
        if (!missing.isEmpty()) {
            throw new BusinessException("標籤不存在：" + missing);
        }
    }

    private void requireUniqueSku(String sku) {
        if (productDAO.existsBySku(sku)) {
            throw new ConflictException("商品貨號已存在（含已刪除）：" + sku);
        }
    }
}
