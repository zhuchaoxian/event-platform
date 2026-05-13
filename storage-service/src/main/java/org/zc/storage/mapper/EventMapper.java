package org.zc.storage.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.zc.storage.entity.EventRecord;

@Mapper
public interface EventMapper extends BaseMapper<EventRecord> {

    int insertBatch(@Param("records") List<EventRecord> records);
}
