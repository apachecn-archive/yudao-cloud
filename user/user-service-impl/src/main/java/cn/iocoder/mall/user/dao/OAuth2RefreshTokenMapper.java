package cn.iocoder.mall.user.dao;

import cn.iocoder.mall.user.dataobject.OAuth2RefreshTokenDO;
import org.springframework.stereotype.Repository;

@Repository
public interface OAuth2RefreshTokenMapper {

    void insert(OAuth2RefreshTokenDO entity);

}