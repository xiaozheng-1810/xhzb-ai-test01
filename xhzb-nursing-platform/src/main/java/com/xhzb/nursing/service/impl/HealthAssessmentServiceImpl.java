package com.xhzb.nursing.service.impl;

import java.util.List;

import cn.hutool.json.JSONUtil;
import com.xhzb.common.exception.base.BaseException;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.nursing.domain.HealthAssessmentDataCollection;
import com.xhzb.nursing.domain.dto.health.ElderAssessmentDto;

import com.xhzb.nursing.service.IHealthAssessmentDataCollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.HealthAssessmentMapper;
import com.xhzb.nursing.domain.HealthAssessment;
import com.xhzb.nursing.service.IHealthAssessmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.Arrays;

/**
 * 健康评估记录Service业务层处理
 * 
 * @author xiaozheng
 * @date 2026-10-28
 */
@Service
public class HealthAssessmentServiceImpl extends ServiceImpl<HealthAssessmentMapper, HealthAssessment> implements IHealthAssessmentService
{
    @Autowired
    private HealthAssessmentMapper healthAssessmentMapper;

    @Autowired
    private IHealthAssessmentDataCollectionService healthAssessmentDataCollectionService;

    /**
     * 查询健康评估记录
     * 
     * @param id 健康评估记录主键
     * @return 健康评估记录
     */
    @Override
    public HealthAssessmentDataCollection selectHealthAssessmentById(Long id)
    {
        return healthAssessmentDataCollectionService.getById(id);
    }

    /**
     * 查询健康评估记录列表
     * 
     * @param healthAssessment 健康评估记录
     * @return 健康评估记录
     */
    @Override
    public List<HealthAssessment> selectHealthAssessmentList(HealthAssessment healthAssessment)
    {
        return healthAssessmentMapper.selectHealthAssessmentList(healthAssessment);
    }

    /**
     * 新增健康评估记录
     * 
     * @param elderAssessmentDto 健康评估记录
     * @return 结果
     */
    @Override
    public long insertOrUpdateHealthAssessment(ElderAssessmentDto elderAssessmentDto)
    {
        //封装健康评估记录对象
        HealthAssessment assessment = new HealthAssessment();
        assessment.setElderName(elderAssessmentDto.getBasicInfo().getElderName());//老人名称
        assessment.setIdCard(elderAssessmentDto.getBasicInfo().getIdCard());//身份证号

        //1. 健康评估记录ID无值处理逻辑
        if (elderAssessmentDto.getId()==null) {
            //1.1 补充新增前对象的额外属性值
            assessment.setCheckInStatus(0);//未入住
            assessment.setEvaluationProgress(0);//评估中
            assessment.setCreateTime(DateUtils.getNowDate());//创建时间

            //1.2 新增健康评估记录
            this.save(assessment);
        } else {
            //2. 健康评估记录ID有值处理逻辑

            //2.1 根据ID查询健康评估记录
            HealthAssessment healthAssessment = this.getById(elderAssessmentDto.getId());

            //2.2 判断健康评估记录是否存在
            if(healthAssessment==null) {
                throw new BaseException("健康评估记录不存在，无法更新");
            }

            //2.3 判断健康评估记录状态是否评估中
            if (healthAssessment.getEvaluationProgress()!=0) {
                throw new BaseException("健康评估记录评估已完成或已取消，无法更新");
            }

            //2.4 更新健康评估记录
            assessment.setId(elderAssessmentDto.getId());
            this.updateById(assessment);
        }

        //3.  封装健康评估数据收集对象
        HealthAssessmentDataCollection collection = new HealthAssessmentDataCollection();
        collection.setId(assessment.getId()); //主键ID
        collection.setBasicInfo(JSONUtil.toJsonStr(elderAssessmentDto.getBasicInfo())); //基本信息
        collection.setHealthAssessment(JSONUtil.toJsonStr(elderAssessmentDto.getHealthAssessmentDto()));//健康评估信息
        collection.setDailyLivingActivities(JSONUtil.toJsonStr(elderAssessmentDto.getDailyLivingActivities()));//日常活动
        collection.setMentalState(JSONUtil.toJsonStr(elderAssessmentDto.getMentalState()));//精神状态
        collection.setPerceptionCommunication(JSONUtil.toJsonStr(elderAssessmentDto.getPerceptionAndCommunication()));//感知觉及沟通
        collection.setSocialParticipation(JSONUtil.toJsonStr(elderAssessmentDto.getSocialParticipation()));//社会参与
        if (elderAssessmentDto.getId()==null) {
            collection.setCreateTime(DateUtils.getNowDate());//创建时间
        }

        //4. 新增或更新健康评估收集数据
        healthAssessmentDataCollectionService.saveOrUpdate(collection);

        //5. 返回健康评估记录主键ID
        return assessment.getId();
    }

/*    *//**
     * 修改健康评估记录
     * 
     * @param healthAssessment 健康评估记录
     * @return 结果
     *//*
    @Override
    public int updateHealthAssessment(HealthAssessment healthAssessment)
    {
        return updateById(healthAssessment)? 1 : 0;
    }*/

    /**
     * 批量删除健康评估记录
     * 
     * @param ids 需要删除的健康评估记录主键
     * @return 结果
     */
    @Override
    public int deleteHealthAssessmentByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids))? 1 : 0;
    }

    /**
     * 删除健康评估记录信息
     * 
     * @param id 健康评估记录主键
     * @return 结果
     */
    @Override
    public int deleteHealthAssessmentById(Long id)
    {
        return removeById(id)? 1 : 0;
    }
}
