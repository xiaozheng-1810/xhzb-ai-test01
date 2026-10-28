package com.xhzb.nursing.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xhzb.common.annotation.Log;
import com.xhzb.common.core.controller.BaseController;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.enums.BusinessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.xhzb.nursing.domain.HealthAssessmentReport;
import com.xhzb.nursing.service.IHealthAssessmentReportService;
import com.xhzb.common.utils.poi.ExcelUtil;
import com.xhzb.common.core.page.TableDataInfo;

/**
 * 健康评估报告Controller
 * 
 * @author xiaozheng
 * @date 2026-10-28
 */
@RestController
@RequestMapping("/nursing/healthReport")
@Tag(name = "健康评估报告相关接口")
public class HealthAssessmentReportController extends BaseController
{
    @Autowired
    private IHealthAssessmentReportService healthAssessmentReportService;

    /**
     * 查询健康评估报告列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthReport:list')")
    @GetMapping("/list")
    @Operation(summary = "查询健康评估报告列表")
    public TableDataInfo list(HealthAssessmentReport healthAssessmentReport)
    {
        startPage();
        List<HealthAssessmentReport> list = healthAssessmentReportService.selectHealthAssessmentReportList(healthAssessmentReport);
        return getDataTable(list);
    }

    /**
     * 导出健康评估报告列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthReport:export')")
    @Log(title = "健康评估报告", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @Operation(summary = "导出健康评估报告列表")
    public void export(HttpServletResponse response, HealthAssessmentReport healthAssessmentReport)
    {
        List<HealthAssessmentReport> list = healthAssessmentReportService.selectHealthAssessmentReportList(healthAssessmentReport);
        ExcelUtil<HealthAssessmentReport> util = new ExcelUtil<HealthAssessmentReport>(HealthAssessmentReport.class);
        util.exportExcel(response, list, "健康评估报告数据");
    }

    /**
     * 获取健康评估报告详细信息
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthReport:query')")
    @GetMapping(value = "/{id}")
    @Operation(summary = "获取健康评估报告详细信息")
    public AjaxResult getInfo(@Schema(name = "健康评估报告ID", requiredMode = Schema.RequiredMode.REQUIRED)
            @PathVariable("id") String id)
    {
        return success(healthAssessmentReportService.selectHealthAssessmentReportById(id));
    }

    /**
     * 新增健康评估报告
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthReport:add')")
    @Log(title = "健康评估报告", businessType = BusinessType.INSERT)
    @PostMapping
    @Operation(summary = "新增健康评估报告")
    public AjaxResult add(@RequestBody HealthAssessmentReport healthAssessmentReport)
    {
        return toAjax(healthAssessmentReportService.insertHealthAssessmentReport(healthAssessmentReport));
    }

    /**
     * 修改健康评估报告
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthReport:edit')")
    @Log(title = "健康评估报告", businessType = BusinessType.UPDATE)
    @PutMapping
    @Operation(summary = "修改健康评估报告")
    public AjaxResult edit(@RequestBody HealthAssessmentReport healthAssessmentReport)
    {
        return toAjax(healthAssessmentReportService.updateHealthAssessmentReport(healthAssessmentReport));
    }

    /**
     * 删除健康评估报告
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthReport:remove')")
    @Log(title = "健康评估报告", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    @Operation(summary = "删除健康评估报告")
    public AjaxResult remove(@Schema(name = "健康评估报告ID", requiredMode = Schema.RequiredMode.REQUIRED) @PathVariable String[] ids)
    {
        return toAjax(healthAssessmentReportService.deleteHealthAssessmentReportByIds(ids));
    }
}
