package com.es.esdemo.mapper;


import com.es.esdemo.pojo.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface GoodsMapper {

    public List<Goods> selectgoods();

}
