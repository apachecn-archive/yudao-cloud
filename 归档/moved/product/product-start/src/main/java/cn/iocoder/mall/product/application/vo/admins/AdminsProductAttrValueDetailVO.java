package cn.iocoder.mall.product.application.vo.admins;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@ApiModel(value = "商品规格值 VO")
@Data
@Accessors(chain = true)
public class AdminsProductAttrValueDetailVO {

    @ApiModelProperty(value = "规格值编号", required = true, example = "1")
    private Integer id;
    @ApiModelProperty(value = "规格值名", required = true, example = "颜色")
    private String name;
    @ApiModelProperty(value = "状态", required = true, example = "1")
    private Integer status;
    @ApiModelProperty(value = "创建时间", required = true, example = "时间戳")
    private LocalDateTime createTime;

}
