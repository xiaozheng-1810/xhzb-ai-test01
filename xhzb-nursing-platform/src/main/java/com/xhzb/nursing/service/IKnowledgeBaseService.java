package com.xhzb.nursing.service;

import java.util.List;
import com.xhzb.nursing.domain.KnowledgeBase;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 知识库Service接口
 * 
 * @author xiaozheng
 * @date 2026-10-27
 */
public interface IKnowledgeBaseService extends IService<KnowledgeBase>
{
    /**
     * 查询知识库
     * 
     * @param id 知识库主键
     * @return 知识库
     */
    public KnowledgeBase selectKnowledgeBaseById(Long id);

    /**
     * 查询知识库列表
     * 
     * @param knowledgeBase 知识库
     * @return 知识库集合
     */
    public List<KnowledgeBase> selectKnowledgeBaseList(KnowledgeBase knowledgeBase);

    /**
     * 新增知识库
     * 
     * @param knowledgeBase 知识库
     * @return 结果
     */
    public int insertKnowledgeBase(KnowledgeBase knowledgeBase);

    /**
     * 修改知识库
     * 
     * @param knowledgeBase 知识库
     * @return 结果
     */
    public int updateKnowledgeBase(KnowledgeBase knowledgeBase);

    /**
     * 批量删除知识库
     * 
     * @param ids 需要删除的知识库主键集合
     * @return 结果
     */
    public int deleteKnowledgeBaseByIds(Long[] ids);

    /**
     * 删除知识库信息
     * 
     * @param id 知识库主键
     * @return 结果
     */
    public int deleteKnowledgeBaseById(Long id);
}
