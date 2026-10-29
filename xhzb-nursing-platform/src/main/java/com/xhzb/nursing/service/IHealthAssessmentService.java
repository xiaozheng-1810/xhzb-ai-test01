package com.xhzb.nursing.service;

import java.util.List;
import com.xhzb.nursing.domain.HealthAssessment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xhzb.nursing.domain.HealthAssessmentReport;
import com.xhzb.nursing.domain.dto.health.ElderAssessmentDto;
import com.xhzb.nursing.domain.vo.ElderInfoVO;

/**
 * 健康评估记录Service接口
 * 
 * @author xiaozheng
 * @date 2026-10-28
 */
public interface IHealthAssessmentService extends IService<HealthAssessment>
{
    /**
     * 查看评估详情（表单回显）
     *
     * @param id 健康评估记录主键
     * @return 表单嵌套结构
     */
    public ElderAssessmentDto selectHealthAssessmentById(Long id);

    /**
     * 查询健康评估记录列表
     * 
     * @param healthAssessment 健康评估记录
     * @return 健康评估记录集合
     */
    public List<HealthAssessment> selectHealthAssessmentList(HealthAssessment healthAssessment);

    /**
     * 新增健康评估记录
     * 
     * @param elderAssessmentDto 健康评估记录
     * @return 结果
     */
    public long insertOrUpdateHealthAssessment(ElderAssessmentDto elderAssessmentDto);

/*    *//**
     * 修改健康评估记录
     * 
     * @param healthAssessment 健康评估记录
     * @return 结果
     *//*
    public int updateHealthAssessment(HealthAssessment healthAssessment);*/

    /**
     * 批量删除健康评估记录
     * 
     * @param ids 需要删除的健康评估记录主键集合
     * @return 结果
     */
    public int deleteHealthAssessmentByIds(Long[] ids);

    /**
     * 删除健康评估记录信息
     * 
     * @param id 健康评估记录主键
     * @return 结果
     */
    public int deleteHealthAssessmentById(Long id);

    /**
     * 取消健康评估
     *
     * @param id 健康评估记录主键
     * @return 结果
     */
    public int cancelHealthAssessment(Long id);

    /**
     * 查看评估结果（同一评估有多条报告时取最新一条）
     *
     * @param assessmentId 健康评估记录主键
     * @return 评估报告，未生成时返回 null
     */
    public HealthAssessmentReport selectLatestReportByAssessmentId(Long assessmentId);

    /**
     * 查看老人信息详情（入住字段填充）
     *
     * @param assessmentId 健康评估记录主键
     * @return 老人入住信息
     */
    public ElderInfoVO selectElderInfoByAssessmentId(Long assessmentId);

    Long assessmentData(ElderAssessmentDto elderAssessmentDto);
}
