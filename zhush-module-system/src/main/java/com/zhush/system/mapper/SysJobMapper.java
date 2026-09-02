package com.zhush.system.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.system.entity.SysJob;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务 Mapper
 *
 * @author zhush
 */
@Mapper
public interface SysJobMapper extends BaseMapperX<SysJob> {

    /**
     * 按任务名称 + 分组查询
     */
    default SysJob selectByJobNameAndGroup(String jobName, String jobGroup) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysJob::getJobName).eq(jobName)
                .and(SysJob::getJobGroup).eq(jobGroup);
        return selectOneByQuery(queryWrapper);
    }
}
