package org.zc.storage.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.zc.storage.entity.CameraRecord;

@Mapper
public interface CameraMapper extends BaseMapper<CameraRecord> {

    int upsertBatch(@Param("records") List<CameraRecord> records);
}
