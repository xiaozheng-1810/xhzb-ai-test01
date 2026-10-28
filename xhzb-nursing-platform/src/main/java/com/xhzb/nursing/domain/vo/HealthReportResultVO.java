package com.xhzb.nursing.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 第二阶段 AI 评估结果（体检报告评估）
 *
 * @author mr.wu
 */
@Data
@Schema(description = "体检报告 AI 评估结果")
public class HealthReportResultVO {

    /** 健康评分 0~100 */
    @Schema(title = "健康评分")
    private Double healthScore;

    /** 风险等级：健康 / 提示 / 风险 / 危险 / 严重危险 */
    @Schema(title = "风险等级")
    private String riskLevel;

    /** 异常数据列表 */
    @Schema(title = "异常数据列表")
    private List<AbnormalDataVO> abnormalData;

    /** 八大系统分值 */
    @Schema(title = "八大系统分值")
    private SystemScore systemScore;

    /** 报告总结 */
    @Schema(title = "报告总结")
    private String summarize;

    /** 嵌套：八大系统分值 */
    @Data
    @Schema(description = "八大系统分值")
    public static class SystemScore {
        @Schema(title = "呼吸系统") private Integer breathingSystem;
        @Schema(title = "消化系统") private Integer digestiveSystem;
        @Schema(title = "内分泌系统") private Integer endocrineSystem;
        @Schema(title = "免疫系统") private Integer immuneSystem;
        @Schema(title = "循环系统") private Integer circulatorySystem;
        @Schema(title = "泌尿系统") private Integer urinarySystem;
        @Schema(title = "运动系统") private Integer motionSystem;
        @Schema(title = "感官系统") private Integer senseSystem;
    }
}