package cn.iocoder.mall.productservice.manager.spu;

import cn.iocoder.common.framework.exception.util.ServiceExceptionUtil;
import cn.iocoder.common.framework.vo.PageResult;
import cn.iocoder.mall.productservice.convert.spu.ProductSpuConvert;
import cn.iocoder.mall.productservice.enums.category.ProductCategoryIdEnum;
import cn.iocoder.mall.productservice.rpc.spu.dto.ProductSpuAndSkuCreateReqDTO;
import cn.iocoder.mall.productservice.rpc.spu.dto.ProductSpuAndSkuUpdateReqDTO;
import cn.iocoder.mall.productservice.rpc.spu.dto.ProductSpuPageReqDTO;
import cn.iocoder.mall.productservice.rpc.spu.dto.ProductSpuRespDTO;
import cn.iocoder.mall.productservice.service.attr.ProductAttrService;
import cn.iocoder.mall.productservice.service.attr.bo.ProductAttrKeyValueBO;
import cn.iocoder.mall.productservice.service.category.ProductCategoryService;
import cn.iocoder.mall.productservice.service.category.bo.ProductCategoryBO;
import cn.iocoder.mall.productservice.service.sku.ProductSkuService;
import cn.iocoder.mall.productservice.service.sku.bo.ProductSkuCreateOrUpdateBO;
import cn.iocoder.mall.productservice.service.spu.ProductSpuService;
import cn.iocoder.mall.productservice.service.spu.bo.ProductSpuBO;
import cn.iocoder.mall.productservice.service.spu.bo.ProductSpuCreateBO;
import cn.iocoder.mall.productservice.service.spu.bo.ProductSpuUpdateBO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.mall.productservice.enums.ProductErrorCodeConstants.*;

/**
* 商品 SPU Manager
*/
@Service
public class ProductSpuManager {

    @Autowired
    private ProductSpuService productSpuService;
    @Autowired
    private ProductSkuService productSkuService;
    @Autowired
    private ProductCategoryService productCategoryService;
    @Autowired
    private ProductAttrService productAttrService;

    /**
    * 创建商品 SPU 和 SKU
    *
    * @param createDTO 创建商品 SPU 和 SKU DTO
    * @return 商品 SPU
    */
    @Transactional
    public Integer createProductSpu(ProductSpuAndSkuCreateReqDTO createDTO) {
        // 校验商品分类是否合法
        this.checkProductCategory(createDTO.getCid());
        // 创建商品 SKU 对象，并进行校验
        List<ProductSkuCreateOrUpdateBO> skuBOs = ProductSpuConvert.INSTANCE.convert(createDTO.getSkus());
        this.checkProductAttr(skuBOs);
        // 插入商品 SPU 记录
        ProductSpuCreateBO spuCreateBO = ProductSpuConvert.INSTANCE.convert(createDTO).setSort(0);
        spuCreateBO.setPrice(skuBOs.stream().min(Comparator.comparing(ProductSkuCreateOrUpdateBO::getPrice)).get().getPrice()); // 求最小价格
        spuCreateBO.setQuantity(skuBOs.stream().mapToInt(ProductSkuCreateOrUpdateBO::getQuantity).sum()); // 求库存之和
        ProductSpuBO spuBO = productSpuService.createProductSpu(spuCreateBO);
        // 插入商品 SKU 记录
        productSkuService.createProductSkus(spuBO.getId(), skuBOs);
        return spuBO.getId();
    }

    /**
    * 更新商品 SPU
    *
    * @param updateDTO 更新商品 SPU DTO
    */
    public void updateProductSpu(ProductSpuAndSkuUpdateReqDTO updateDTO) {
        // 校验商品分类是否合法
        this.checkProductCategory(updateDTO.getCid());
        // 创建商品 SKU 对象，并进行校验
        List<ProductSkuCreateOrUpdateBO> skuBOs = ProductSpuConvert.INSTANCE.convert02(updateDTO.getSkus());
        this.checkProductAttr(skuBOs);
        // 更新商品 SPU 记录
        ProductSpuUpdateBO spuUpdateBO = ProductSpuConvert.INSTANCE.convert(updateDTO);
        spuUpdateBO.setPrice(skuBOs.stream().min(Comparator.comparing(ProductSkuCreateOrUpdateBO::getPrice)).get().getPrice()); // 求最小价格
        spuUpdateBO.setQuantity(skuBOs.stream().mapToInt(ProductSkuCreateOrUpdateBO::getQuantity).sum()); // 求库存之和
        productSpuService.updateProductSpu(spuUpdateBO);
        // 更新商品 SKU 记录
        productSkuService.updateProductSkus(updateDTO.getId(), skuBOs);
    }

    /**
    * 获得商品 SPU
    *
    * @param productSpuId 商品 SPU编号
    * @return 商品 SPU
    */
    public ProductSpuRespDTO getProductSpu(Integer productSpuId) {
        ProductSpuBO productSpuBO = productSpuService.getProductSpu(productSpuId);
        return ProductSpuConvert.INSTANCE.convert(productSpuBO);
    }

    /**
    * 获得商品 SPU列表
    *
    * @param productSpuIds 商品 SPU编号列表
    * @return 商品 SPU列表
    */
    public List<ProductSpuRespDTO> listProductSpus(List<Integer> productSpuIds) {
        List<ProductSpuBO> productSpuBOs = productSpuService.listProductSpus(productSpuIds);
        return ProductSpuConvert.INSTANCE.convertList02(productSpuBOs);
    }

    /**
    * 获得商品 SPU分页
    *
    * @param pageDTO 商品 SPU分页查询
    * @return 商品 SPU分页结果
    */
    public PageResult<ProductSpuRespDTO> pageProductSpu(ProductSpuPageReqDTO pageDTO) {
        PageResult<ProductSpuBO> pageResultBO = productSpuService.pageProductSpu(ProductSpuConvert.INSTANCE.convert(pageDTO));
        return ProductSpuConvert.INSTANCE.convertPage(pageResultBO);
    }

    /**
     * 添加或修改商品 SPU 时，校验商品分类是否合法
     *
     * @param cid 商品分类编号
     * @return 商品分类
     */
    private ProductCategoryBO checkProductCategory(Integer cid) {
        ProductCategoryBO categoryBO = productCategoryService.getProductCategory(cid);
        if (categoryBO == null) {
            // 不存在
            throw ServiceExceptionUtil.exception(PRODUCT_CATEGORY_NOT_EXISTS);
        }
        if (ProductCategoryIdEnum.ROOT.getId().equals(categoryBO.getPid())) {
            // 商品只能添加到二级分类下
            throw ServiceExceptionUtil.exception(PRODUCT_SPU_CATEGORY_MUST_BE_LEVEL2);
        }
        return categoryBO;
    }

    private List<ProductAttrKeyValueBO> checkProductAttr(List<ProductSkuCreateOrUpdateBO> skuBOs) {
        // 第一步，校验 SKU 使用到的规格是否存在
        Set<Integer> attrValueIds = new HashSet<>();
        skuBOs.forEach(sku -> attrValueIds.addAll(sku.getAttrValueIds()));
        List<ProductAttrKeyValueBO> attrKeyValueBOs = productAttrService.validProductAttr(attrValueIds, true);
        // 第二步，校验 SKU 设置的规格是否合法，例如说数量是否一致，是否重复等等
        // 创建 ProductAttrDetailBO 的映射。其中，KEY 为 ProductAttrDetailBO.attrValueId ，即规格值的编号
        Map<Integer, ProductAttrKeyValueBO> productAttrDetailBOMap = attrKeyValueBOs.stream().collect(
                Collectors.toMap(ProductAttrKeyValueBO::getAttrValueId, productAttrDetailBO -> productAttrDetailBO));
        // 1. 先校验，一个 Sku 下，没有重复的规格。校验方式是，遍历每个 Sku ，看看是否有重复的规格 attrId
        for (ProductSkuCreateOrUpdateBO sku : skuBOs) {
            Set<Integer> attrIds = sku.getAttrValueIds().stream().map(attrValueId -> productAttrDetailBOMap.get(attrValueId).getAttrKeyId())
                    .collect(Collectors.toSet());
            if (attrIds.size() != sku.getAttrValueIds().size()) {
                throw ServiceExceptionUtil.exception(PRODUCT_SKU_ATTR_CANT_NOT_DUPLICATE);
            }
        }
        // 2. 再校验，每个 Sku 的规格值的数量，是一致的。
        int attrValueIdsSize = skuBOs.get(0).getAttrValueIds().size();
        for (int i = 1; i < skuBOs.size(); i++) {
            if (attrValueIdsSize != skuBOs.get(i).getAttrValueIds().size()) {
                throw ServiceExceptionUtil.exception(PRODUCT_SPU_ATTR_NUMBERS_MUST_BE_EQUALS);
            }
        }
        // 3. 最后校验，每个 Sku 之间不是重复的
        Set<Set<Integer>> skuAttrValues = new HashSet<>(); // 每个元素，都是一个 Sku 的 attrValueId 集合。这样，通过最外层的 Set ，判断是否有重复的.
        for (ProductSkuCreateOrUpdateBO sku : skuBOs) {
            if (!skuAttrValues.add(new HashSet<>(sku.getAttrValueIds()))) { // 添加失败，说明重复
                throw ServiceExceptionUtil.exception(PRODUCT_SPU_SKU_NOT_DUPLICATE);
            }
        }
        return attrKeyValueBOs;
    }

}