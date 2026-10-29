package com.xhzb.nursing.service.impl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhzb.common.exception.base.BaseException;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.common.utils.PDFUtil;
import com.xhzb.nursing.constant.PromptConstant;
import com.xhzb.nursing.domain.HealthAssessment;
import com.xhzb.nursing.domain.HealthAssessmentDataCollection;
import com.xhzb.nursing.domain.HealthAssessmentReport;
import com.xhzb.nursing.domain.dto.health.BasicInfo;
import com.xhzb.nursing.domain.dto.health.DailyLivingActivities;
import com.xhzb.nursing.domain.dto.health.ElderAssessmentDto;
import com.xhzb.nursing.domain.dto.health.HealthAssessmentDto;
import com.xhzb.nursing.domain.dto.health.MentalState;
import com.xhzb.nursing.domain.dto.health.PerceptionAndCommunication;
import com.xhzb.nursing.domain.dto.health.SocialParticipation;
import com.xhzb.nursing.domain.vo.ElderInfoVO;
import com.xhzb.nursing.domain.vo.ElderLevelResultVO;
import com.xhzb.nursing.domain.vo.HealthReportResultVO;
import com.xhzb.nursing.mapper.HealthAssessmentMapper;
import com.xhzb.nursing.service.IHealthAssessmentDataCollectionService;
import com.xhzb.nursing.service.IHealthAssessmentReportService;
import com.xhzb.nursing.service.IHealthAssessmentService;
import com.xhzb.oss.client.OSSAliyunFileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 健康评估记录Service业务层处理
 * 
 * @author xiaozheng
 * @date 2026-10-28
 */
@Slf4j
@Service
public class HealthAssessmentServiceImpl extends ServiceImpl<HealthAssessmentMapper, HealthAssessment> implements IHealthAssessmentService
{
    @Autowired
    private HealthAssessmentMapper healthAssessmentMapper;

    @Autowired
    private IHealthAssessmentDataCollectionService healthAssessmentDataCollectionService;

    /**
     * 查看评估详情（表单回显）
     *
     * @param id 健康评估记录主键
     * @return 表单嵌套结构
     */
    @Override
    public ElderAssessmentDto selectHealthAssessmentById(Long id)
    {
        // 采集表主键与评估主键相同，直接按 id 查询
        HealthAssessmentDataCollection collection = healthAssessmentDataCollectionService.getById(id);
        if (collection == null) {
            throw new BaseException("评估表单数据不存在");
        }

        ElderAssessmentDto dto = new ElderAssessmentDto();
        dto.setId(collection.getId());
        dto.setBasicInfo(parseFormJson(collection.getBasicInfo(), BasicInfo.class));
        dto.setHealthAssessmentDto(parseFormJson(collection.getHealthAssessment(), HealthAssessmentDto.class));
        dto.setDailyLivingActivities(parseFormJson(collection.getDailyLivingActivities(), DailyLivingActivities.class));
        dto.setMentalState(parseFormJson(collection.getMentalState(), MentalState.class));
        dto.setPerceptionAndCommunication(parseFormJson(collection.getPerceptionCommunication(), PerceptionAndCommunication.class));
        dto.setSocialParticipation(parseFormJson(collection.getSocialParticipation(), SocialParticipation.class));
        return dto;
    }

    /**
     * 解析采集表中存储的表单 JSON
     *
     * @param json  表单 JSON 字符串
     * @param clazz 目标类型
     * @return 解析结果，空值或解析失败时返回 null
     */
    private <T> T parseFormJson(String json, Class<T> clazz)
    {
        if (StrUtil.isBlank(json) || "null".equalsIgnoreCase(json.trim())) {
            return null;
        }
        try {
            return JSONUtil.toBean(json, clazz);
        } catch (Exception e) {
            log.warn("解析健康评估表单数据失败，json={}", json, e);
            return null;
        }
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
            //2. 健康评估记录ID有值处理逻辑(实质变为修改)
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
            // 更新的时候前端会传入健康评估记录ID，所以这里直接使用传入的ID
            assessment.setId(elderAssessmentDto.getId());
            this.updateById(assessment);
        }

        //3.  封装健康评估数据采集对象
        HealthAssessmentDataCollection collection = new HealthAssessmentDataCollection();
        collection.setId(assessment.getId()); //主键ID
        // hutool工具中的json转换方法,进行序列化把对象转换成json字符串
        // eg: {"name":"张三","age":78,"address":"XX养老院"}
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
     * 批量删除健康评估记录（级联删除采集明细和评估报告）
     *
     * @param ids 需要删除的健康评估记录主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteHealthAssessmentByIds(Long[] ids)
    {
        return doDelete(ids);
    }

    /**
     * 删除健康评估记录信息（级联删除采集明细和评估报告）
     *
     * @param id 健康评估记录主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteHealthAssessmentById(Long id)
    {
        return doDelete(new Long[]{id});
    }

    /**
     * 级联删除评估主记录、采集明细和评估报告
     *
     * @param ids 健康评估记录主键数组
     * @return 结果
     */
    private int doDelete(Long[] ids)
    {
        if (ids == null || ids.length == 0) {
            throw new BaseException("请选择要删除的健康评估记录");
        }

        List<Long> idList = Arrays.asList(ids);
        // 部分 ID 不存在时删除存在的记录，全部不存在时给出明确提示
        long count = this.count(Wrappers.<HealthAssessment>lambdaQuery().in(HealthAssessment::getId, idList));
        if (count == 0) {
            throw new BaseException("健康评估记录不存在");
        }

        // 先删子表，再删主表，数据库无外键，级联完整性由这里保证
        reportService.remove(Wrappers.<HealthAssessmentReport>lambdaQuery()
                .in(HealthAssessmentReport::getHealthAssessmentId, idList));
        healthAssessmentDataCollectionService.removeByIds(idList);
        return removeByIds(idList) ? 1 : 0;
    }

    /**
     * 取消健康评估
     *
     * @param id 健康评估记录主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelHealthAssessment(Long id)
    {
        HealthAssessment assessment = this.getById(id);
        if (assessment == null) {
            throw new BaseException("健康评估记录不存在");
        }
        if (Objects.equals(assessment.getCheckInStatus(), 1)) {
            throw new BaseException("老人已入住，无法取消评估");
        }

        Integer progress = assessment.getEvaluationProgress();
        if (progress == null) {
            throw new BaseException("评估进度数据异常，无法取消");
        }
        if (progress == 1) {
            throw new BaseException("评估已完成，无法取消");
        }
        if (progress == 2) {
            throw new BaseException("评估已取消，请勿重复操作");
        }

        // 条件更新，避免并发操作把已变更的状态覆盖掉
        HealthAssessment update = new HealthAssessment();
        update.setEvaluationProgress(2);
        boolean updated = this.update(update, Wrappers.<HealthAssessment>lambdaUpdate()
                .eq(HealthAssessment::getId, id)
                .eq(HealthAssessment::getEvaluationProgress, 0));
        if (!updated) {
            throw new BaseException("评估状态已变更，请刷新后重试");
        }
        return 1;
    }

    /**
     * 查看评估结果（同一评估有多条报告时取最新一条）
     *
     * @param assessmentId 健康评估记录主键
     * @return 评估报告，未生成时返回 null
     */
    @Override
    public HealthAssessmentReport selectLatestReportByAssessmentId(Long assessmentId)
    {
        if (this.getById(assessmentId) == null) {
            throw new BaseException("健康评估记录不存在");
        }
        return reportService.getOne(Wrappers.<HealthAssessmentReport>lambdaQuery()
                .eq(HealthAssessmentReport::getHealthAssessmentId, assessmentId)
                .orderByDesc(HealthAssessmentReport::getId)
                .last("LIMIT 1"));
    }

    /**
     * 查看老人信息详情（入住字段填充）
     *
     * @param assessmentId 健康评估记录主键
     * @return 老人入住信息
     */
    @Override
    public ElderInfoVO selectElderInfoByAssessmentId(Long assessmentId)
    {
        HealthAssessment assessment = this.getById(assessmentId);
        if (assessment == null) {
            throw new BaseException("健康评估记录不存在");
        }

        ElderInfoVO vo = new ElderInfoVO();
        // 主表字段先作为兜底值
        vo.setCoreSuggestion(assessment.getCoreSuggestion());
        vo.setName(assessment.getElderName());
        vo.setIdCardNo(assessment.getIdCard());

        HealthAssessmentDataCollection collection = healthAssessmentDataCollectionService.getById(assessmentId);
        if (collection == null) {
            log.warn("健康评估采集数据不存在，assessmentId={}", assessmentId);
            return vo;
        }

        BasicInfo basicInfo = parseFormJson(collection.getBasicInfo(), BasicInfo.class);
        if (basicInfo == null) {
            log.warn("健康评估基本信息为空或解析失败，assessmentId={}", assessmentId);
            return vo;
        }

        vo.setPhone(basicInfo.getElderContact());
        vo.setMedicalPaymentMethod(basicInfo.getMedicalPaymentMethod());
        vo.setNation(basicInfo.getNation());
        vo.setEducationLevel(basicInfo.getEducationLevel());
        vo.setSocialSecurityCard(basicInfo.getSocialSecurityCard());
        vo.setLivingSituation(basicInfo.getLivingSituation());
        vo.setReligiousBelief(basicInfo.getReligiousBelief());
        vo.setEconomicSource(basicInfo.getEconomicSource());
        vo.setMaritalStatus(basicInfo.getMaritalStatus());
        if (StrUtil.isNotBlank(basicInfo.getElderName())) {
            vo.setName(basicInfo.getElderName());
        }
        if (StrUtil.isNotBlank(basicInfo.getIdCard())) {
            vo.setIdCardNo(basicInfo.getIdCard());
        }
        return vo;
    }

    @Autowired
    private ChatClient healthChatClient;

    @Autowired
    private IHealthAssessmentReportService reportService;

    @Autowired
    private OSSAliyunFileStorageService storageService;
    @Override
    public Long assessmentData(ElderAssessmentDto elderAssessmentDto) {
        // 完整评估流程步骤
        // 1.更新保存评估数据: 接收前端提交的老人评估表单，把本次评估的基础表单数据保存或者更新到数据库。
        // 新增时数据库自增主键已由 MyBatis-Plus 回填到主记录实体，这里直接使用方法返回值，后续报告和主记录更新都基于该 ID
        long assessmentId = this.insertOrUpdateHealthAssessment(elderAssessmentDto);

        // 2.提取各项能力数据: 从表单里面抽取出四大模块评估结果： 日常生活活动等级、精神状态等级、感知觉与沟通等级、社会参与等级。 把 “能力完好、轻度受损、中度受损、重度受损” 这类文字等级转换为对应数字编码。 同时提取配套风险信息：近 30 天跌倒、噎食、自杀尝试、走失、昏迷发生次数，是否患有痴呆、精神疾病，画钟测试结果，全部收集齐全，供 AI 分析使用。
        String dailyLivingLevel = this.getLevel(elderAssessmentDto.getDailyLivingActivities().getAbilityRating());
        String mentalLevel = this.getLevel(elderAssessmentDto.getMentalState().getAbilityRating());
        String perceptionLevel = this.getLevel(elderAssessmentDto.getPerceptionAndCommunication().getAbilityRating());
        String socialLevel = this.getLevel(elderAssessmentDto.getSocialParticipation().getAbilityRating());
        Integer fallCount = elderAssessmentDto.getHealthAssessmentDto().getRecent30Days().getFall();
        Integer chockingCount = elderAssessmentDto.getHealthAssessmentDto().getRecent30Days().getChoking();
        Integer suicideCount = elderAssessmentDto.getHealthAssessmentDto().getRecent30Days().getSuicideAttempt();
        Integer lostCount = elderAssessmentDto.getHealthAssessmentDto().getRecent30Days().getLost();
        Integer comaCount = elderAssessmentDto.getHealthAssessmentDto().getRecent30Days().getComa();
        int dementia = elderAssessmentDto.getHealthAssessmentDto().getDiseaseDiagnosis().getDementia().equals("无") ? 0 : 1;
        int mentalIllness = elderAssessmentDto.getHealthAssessmentDto().getDiseaseDiagnosis().getMentalIllness().equals("无") ? 0 : 1;
        int clockDrawingTest = elderAssessmentDto.getMentalState().getClockDrawingTest().getResult() == 2 ? 1 : 0;

        // 3.第一阶段 AI分析老人多维度能力评估
        //    1）使用收集好的老人各项评估信息，拼接组装 AI 提示词 prompt，带上评估规则。
        String healthPrompt = String.format(PromptConstant.HEALTH_PROMPT, dailyLivingLevel, mentalLevel, perceptionLevel, socialLevel, fallCount, chockingCount, suicideCount,lostCount, comaCount, dementia, mentalIllness, clockDrawingTest);
        //    2）通过 ChatClient 调用大模型做能力分析。
        /*
        * 1. **healthChatClient**：预先配置好的 SpringAI 对话客户端，里面绑定了模型地址、密钥、系统提示词、参数
        * 2. **.prompt()**：新建一次对话请求构造器，开始组装这次给大模型的请求
        * 3. **.user(healthPrompt)**：设置【用户消息】，`healthPrompt`就是你拼好的评估文本、要求模型输出 JSON 的指令
        * 4. **.call()**：**同步发起请求，阻塞等待大模型返回完整结果**
        * 5. **.content()**：直接提取模型返回的文本字符串；模型无输出时会返回 null
        一句话串起来：用预先配置好的 AI 客户端，组装用户提示词，同步调用模型，拿到模型返回的文本。*/
        String healthResponse = healthChatClient.prompt().user(healthPrompt).call().content();
        //    3）处理大模型返回内容，剔除多余文字，只解析出 JSON 格式结果，拿到老人初始能力等级、最终能力等级、等级变化原因。
        // **把 JSON 格式的字符串 `healthResponse`，转成 Java 对象 `ElderLevelResultVO`**，也就是反序列化。
        ElderLevelResultVO levelResult = JSONUtil.toBean(healthResponse, ElderLevelResultVO.class);
        //    4）新建一条健康评估报告记录，把评估基础信息、第一阶段 AI 的分析结果先存入数据库。
        HealthAssessmentReport report = new HealthAssessmentReport();
        report.setHealthAssessmentId(assessmentId);
        report.setDailyActivityLevel(dailyLivingLevel);
        report.setMentalStatusLevel(mentalLevel);
        report.setPerceptionCommunicationLevel(perceptionLevel);
        report.setSocialParticipationLevel(socialLevel);
        report.setInitialAbilityLevel(levelResult.getPreLevel());
        report.setFinalAbilityLevel(levelResult.getFinalLevel());
        report.setLevelChangeReason(levelResult.getReason());
        report.setCreateTime(new Date());

//        4.第二阶段 AI 分析：体检报告分析
//        1）从阿里云 OSS 文件存储下载老人上传的体检 PDF 报告文件。
        InputStream inputStream = storageService.download(elderAssessmentDto.getHealthAssessmentDto().getRecent30Days().getMedicalReport());
//        2）读取解析 PDF，提取出体检报告全部文本内容。
        String healthReport = PDFUtil.pdfToString(inputStream);
//        3）将体检文本内容组装成第二套 AI 提示词 prompt。
        String healthReportPrompt = String.format(PromptConstant.HEALTH_REPORT_PROMPT, healthReport);
//        4）调用大模型对体检报告做专业分析。
        String healthReportResponse = healthChatClient.prompt().user(healthReportPrompt).call().content();
//        5）解析 AI 返回结果，拿到健康评分、风险等级、报告总结、身体异常项、各身体系统评分。
        HealthReportResultVO healthReportResult = JSONUtil.toBean(healthReportResponse, HealthReportResultVO.class);

//        5.保存评估结果到评估结果表,把两阶段 AI 产出的全部分析结果回填到评估报告记录中。
//        业务判断规则：
//        1）是否建议入住判断规则：健康评分≥60 分，标记为建议入住；低于 60 不建议入住。
        int coreSuggestion = healthReportResult.getHealthScore() >= 60 ? 1 : 0;
        report.setCoreSuggestion(coreSuggestion);
//        2）依据健康分数计算建议护理等级：90 分以上三级护理，80 以上二级，70 以上一级，60 以上特级，低于 60 不建议入住。
        report.setRecommendedCareLevel(getRecommendedCareLevel(healthReportResult.getHealthScore()));
//        3）评估报告表记录入住状态统一标记为未入住。
        report.setCheckInStatus(0);
//        4）健康评估报告表所有字段根据实际情况，该有值的尽量都赋值
        report.setHealthScore(healthReportResult.getHealthScore().toString()); // 设置健康评分
        report.setRiskLevel(healthReportResult.getRiskLevel()); // 设置风险等级
        report.setReportSummary(healthReportResult.getSummarize()); // 设置报告总结
        report.setAbnormalAnalysis(JSONUtil.toJsonStr(healthReportResult.getAbnormalData())); // 设置异常分析
        report.setSystemScore(JSONUtil.toJsonStr(healthReportResult.getSystemScore())); // 设置八大系统分值
        reportService.save(report);

//        6.更新评估主记录的状态与入住建议，修改评估主表数据，写入是否建议入住的标记，把评估进度设置为【评估已完成】。
        HealthAssessment assessment = new HealthAssessment();
        assessment.setId(assessmentId);
        assessment.setCheckInStatus(0); // 入住状态设置为：未入住
        assessment.setCoreSuggestion(coreSuggestion); // 是否建议入住
        assessment.setEvaluationProgress(1); // 评估进度设置为：已完成
        this.updateById(assessment); // 更新评估主记录

//        7.返回评估 ID 整个业务流程结束，返回本次评估记录的唯一ID。
        return assessmentId;
    }

    public String getLevel(String ability){
        String level = "";
        switch (ability){
            case "能力完好":
                level = "0";
                break;
            case "轻度受损":
                level = "1";
                break;
            case "中度受损":
                level = "2";
                break;
            case "重度受损":
                level = "3";
                break;
        }
        return level;
    }

    // 2）依据健康分数计算建议护理等级：90 分以上三级护理，80 以上二级，70 以上一级，60 以上特级，低于 60 不建议入住。
    private String getRecommendedCareLevel(Double healthScore){
        if (healthScore >= 90){
            return "三级护理";
        }else if (healthScore >= 80){
            return "二级护理";
        }else if (healthScore >= 70){
            return "一级护理";
        }else if (healthScore >= 60){
            return "特级护理";
        }
        return "不建议入住";
    }
}
