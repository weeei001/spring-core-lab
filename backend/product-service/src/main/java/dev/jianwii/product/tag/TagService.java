package dev.jianwii.product.tag;

import dev.jianwii.api.product.TagQueryParameter;
import dev.jianwii.common.exception.ConflictException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.product.Product;
import dev.jianwii.entity.product.Tag;
import dev.jianwii.product.product.ProductDAO;
import org.springframework.stereotype.Service;

/** 標籤 CRUD；有商品使用時不可刪除。 */
@Service
public class TagService extends BaseService<Tag, TagQueryParameter> {
    // 用 DAO 而非 ProductService，避免兩個 Service 互相注入
    private final ProductDAO productDAO;

    public TagService(TagDAO tagDAO, ProductDAO productDAO) {
        super(tagDAO);
        this.productDAO = productDAO;
    }

    @Override
    protected void beforeDelete(Tag entity) {
        if (productDAO.existsByTagIdsContainingAndDbMark(entity.getId(), Product.VALID)) {
            throw new ConflictException("仍有商品使用此標籤：" + entity.getName());
        }
    }
}
