package com.xhzb.nursing.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 第一阶段 AI 评估结果（多维度能力等级评估）
 *
 * @author mr.wu
 */
@Data
@Schema(description = "老人能力等级评估结果")
public class ElderLevelResultVO {

    /** 初步能力等级：能力完好 / 轻度失能 / 中度失能 / 重度失能 */
    @Schema(title = "初步能力等级")
    private String preLevel;

    /** 最终能力等级：能力完好 / 轻度失能 / 中度失能 / 重度失能 */
    @Schema(title = "最终能力等级")
    private String finalLevel;

    /** 等级变更依据（命中评估原则 2 中的若干条） */
    @Schema(title = "等级变更依据")
    private String reason;
}