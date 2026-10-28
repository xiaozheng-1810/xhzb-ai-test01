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
import com.xhzb.nursing.domain.HealthAssessmentDataCollection;
import com.xhzb.nursing.service.IHealthAssessmentDataCollectionService;
import com.xhzb.common.utils.poi.ExcelUtil;
import com.xhzb.common.core.page.TableDataInfo;

/**
 * 健康评估数据采集Controller
 * 
 * @author xiaozheng
 * @date 2026-10-28
 */
@RestController
@RequestMapping("/nursing/healthCollection")
@Tag(name = "健康评估数据采集相关接口")
public class HealthAssessmentDataCollectionController extends BaseController
{
    @Autowired
    private IHealthAssessmentDataCollectionService healthAssessmentDataCollectionService;

    /**
     * 查询健康评估数据采集列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthCollection:list')")
    @GetMapping("/list")
    @Operation(summary = "查询健康评估数据采集列表")
    public TableDataInfo list(HealthAssessmentDataCollection healthAssessmentDataCollection)
    {
        startPage();
        List<HealthAssessmentDataCollection> list = healthAssessmentDataCollectionService.selectHealthAssessmentDataCollectionList(healthAssessmentDataCollection);
        return getDataTable(list);
    }

    /**
     * 导出健康评估数据采集列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthCollection:export')")
    @Log(title = "健康评估数据采集", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @Operation(summary = "导出健康评估数据采集列表")
    public void export(HttpServletResponse response, HealthAssessmentDataCollection healthAssessmentDataCollection)
    {
        List<HealthAssessmentDataCollection> list = healthAssessmentDataCollectionService.selectHealthAssessmentDataCollectionList(healthAssessmentDataCollection);
        ExcelUtil<HealthAssessmentDataCollection> util = new ExcelUtil<HealthAssessmentDataCollection>(HealthAssessmentDataCollection.class);
        util.exportExcel(response, list, "健康评估数据采集数据");
    }

    /**
     * 获取健康评估数据采集详细信息
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthCollection:query')")
    @GetMapping(value = "/{id}")
    @Operation(summary = "获取健康评估数据采集详细信息")
    public AjaxResult getInfo(@Schema(name = "健康评估数据采集ID", requiredMode = Schema.RequiredMode.REQUIRED)
            @PathVariable("id") Long id)
    {
        return success(healthAssessmentDataCollectionService.selectHealthAssessmentDataCollectionById(id));
    }

    /**
     * 新增健康评估数据采集
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthCollection:add')")
    @Log(title = "健康评估数据采集", businessType = BusinessType.INSERT)
    @PostMapping
    @Operation(summary = "新增健康评估数据采集")
    public AjaxResult add(@RequestBody HealthAssessmentDataCollection healthAssessmentDataCollection)
    {
        return toAjax(healthAssessmentDataCollectionService.insertHealthAssessmentDataCollection(healthAssessmentDataCollection));
    }

    /**
     * 修改健康评估数据采集
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthCollection:edit')")
    @Log(title = "健康评估数据采集", businessType = BusinessType.UPDATE)
    @PutMapping
    @Operation(summary = "修改健康评估数据采集")
    public AjaxResult edit(@RequestBody HealthAssessmentDataCollection healthAssessmentDataCollection)
    {
        return toAjax(healthAssessmentDataCollectionService.updateHealthAssessmentDataCollection(healthAssessmentDataCollection));
    }

    /**
     * 删除健康评估数据采集
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthCollection:remove')")
    @Log(title = "健康评估数据采集", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    @Operation(summary = "删除健康评估数据采集")
    public AjaxResult remove(@Schema(name = "健康评估数据采集ID", requiredMode = Schema.RequiredMode.REQUIRED) @PathVariable Long[] ids)
    {
        return toAjax(healthAssessmentDataCollectionService.deleteHealthAssessmentDataCollectionByIds(ids));
    }
}
