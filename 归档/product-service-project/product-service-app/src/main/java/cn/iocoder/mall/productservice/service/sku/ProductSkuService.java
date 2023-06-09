package cn.iocoder.mall.productservice.service.sku;

import cn.iocoder.common.framework.enums.CommonStatusEnum;
import cn.iocoder.common.framework.util.CollectionUtils;
import cn.iocoder.common.framework.util.StringUtils;
import cn.iocoder.mall.productservice.convert.sku.ProductSkuConvert;
import cn.iocoder.mall.productservice.dal.mysql.dataobject.sku.ProductSkuDO;
import cn.iocoder.mall.productservice.dal.mysql.mapper.sku.ProductSkuMapper;
import cn.iocoder.mall.productservice.service.sku.bo.ProductSkuBO;
import cn.iocoder.mall.productservice.service.sku.bo.ProductSkuCreateOrUpdateBO;
import cn.iocoder.mall.productservice.service.sku.bo.ProductSkuListQueryBO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductSkuService {

    @Autowired
    private ProductSkuMapper productSkuMapper;

    /**
     * 获得 sku 数组中，指定规格的 sku
     *
     * @param attrValueIds 指定规格 Value 的编号数组
     * @param skus sku 数组
     * @return 符合条件的 sku
     */
    private ProductSkuDO findProductSku(Collection<Integer> attrValueIds, List<ProductSkuDO> skus) {
        if (CollectionUtils.isEmpty(skus)) {
            return null;
        }
        // 创建成 Set ，方便后面比较
        attrValueIds = new HashSet<>(attrValueIds);
        for (ProductSkuDO sku : skus) {
            Set<Integer> skuAttrValueIds = StringUtils.split(sku.getAttrs(), ",")
                    .stream().map(Integer::parseInt).collect(Collectors.toSet());
            if (attrValueIds.equals(skuAttrValueIds)) {
                return sku;
            }
        }
        return null;
    }

    /**
     * 获得商品 SKU 列表
     *
     * @param queryBO 商品 SKU 列表查询条件 BO
     * @return 商品 SKU 列表
     */
    public List<ProductSkuBO> listProductSkus(ProductSkuListQueryBO queryBO) {
        // TODO FROM 芋艿：可能要考虑下，是不是要必须传递条件
        List<ProductSkuDO> productSkuDOs = productSkuMapper.selectList(queryBO);
        return ProductSkuConvert.INSTANCE.convertList02(productSkuDOs);
    }

}
