package com.xhzb.tool;

import com.xhzb.nursing.domain.vo.NursingLevelVo;
import com.xhzb.nursing.domain.vo.NursingProjectVo;
import com.xhzb.nursing.service.INursingProjectService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NursingProjectTool {

    @Autowired
    private INursingProjectService iNursingProjectService;

    @Tool(name = "find_project_tool", description = "查询所有护理项目")
    public List<NursingProjectVo> findAllNursingProject(){

        List<NursingProjectVo> nursingProjectVos = iNursingProjectService.listAll();
        return nursingProjectVos;
    }
}
