package com.xhzb.nursing.service.impl;

import java.io.InputStream;
import java.util.List;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.oss.client.OSSAliyunFileStorageService;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.KnowledgeBaseMapper;
import com.xhzb.nursing.domain.KnowledgeBase;
import com.xhzb.nursing.service.IKnowledgeBaseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.Arrays;

/**
 * 知识库Service业务层处理
 * 
 * @author xiaozheng
 * @date 2026-10-27
 */
@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements IKnowledgeBaseService
{
    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    /**
     * 查询知识库
     * 
     * @param id 知识库主键
     * @return 知识库
     */
    @Override
    public KnowledgeBase selectKnowledgeBaseById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询知识库列表
     * 
     * @param knowledgeBase 知识库
     * @return 知识库
     */
    @Override
    public List<KnowledgeBase> selectKnowledgeBaseList(KnowledgeBase knowledgeBase)
    {
        return knowledgeBaseMapper.selectKnowledgeBaseList(knowledgeBase);
    }

    /**
     * 新增知识库
     * 
     * @param knowledgeBase 知识库
     * @return 结果
     */

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private OSSAliyunFileStorageService ossAliyunFileStorageService;
    @Override
    public int insertKnowledgeBase(KnowledgeBase knowledgeBase)
    {
        // 从阿里oss中下载添加的文档
        // 1.下载文档并读取内容为输入流
        InputStream inputStream = ossAliyunFileStorageService.download(knowledgeBase.getDocumentUrl());
        // 2.创建输入流资源
        // InputStreamResource的作用:适配器，把 JDK 原生InputStream包装成 Spring 统一的Resource规范对象。
        // 之所以必须包装，根本原因：SpringAI 的PDFReader等组件，方法签名设计的就是接收Resource，不接收InputStream。
        InputStreamResource inputStreamResource = new InputStreamResource(inputStream);

        // 实现按页读pdf文档
        PagePdfDocumentReader pagePdfDocumentReader = new PagePdfDocumentReader(inputStreamResource, PdfDocumentReaderConfig.builder()
                .withPageTopMargin(0)
                .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                        .withNumberOfBottomTextLinesToDelete(0)
                        .build())
                .withPagesPerDocument(1)
                .build());

        // 切分文档为chunk
        List<Document> documentList = pagePdfDocumentReader.read();
        TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
                .withChunkSize(500)
                .withMinChunkSizeChars(200)
                .withMinChunkLengthToEmbed(10)
                .withMaxNumChunks(10000)
                .withKeepSeparator(false)
                .build();
        List<Document> documents = tokenTextSplitter.apply(documentList);

        // 将chunk向量化并添加到向量数据库中
        vectorStore.add(documents);

        // 收集所有文档id
        List<String> docIdlist = documents.stream().map(Document::getId).toList();
        String docIdListJSON = JSONUtil.toJsonStr(docIdlist);

        // 将文档保存到mysql数据库
        knowledgeBase.setCreateTime(DateUtils.getNowDate());
        knowledgeBase.setRemark(docIdListJSON);
        boolean flag = this.save(knowledgeBase);

        return flag ? 1 :0 ;
    }

    /**
     * 修改知识库
     * 
     * @param knowledgeBase 知识库
     * @return 结果
     */
    @Override
    public int updateKnowledgeBase(KnowledgeBase knowledgeBase)
    {
        return updateById(knowledgeBase)? 1 : 0;
    }

    /**
     * 批量删除知识库
     * 
     * @param ids 需要删除的知识库主键
     * @return 结果
     */
    @Override
    public int deleteKnowledgeBaseByIds(Long[] ids)
    {
        // 查询出所有的知识库
        List<KnowledgeBase> knowledgeBaseList = this.listByIds(Arrays.asList(ids));
        if (CollectionUtils.isEmpty(knowledgeBaseList)) {
            return 0;
        }

        for (KnowledgeBase knowledgeBase : knowledgeBaseList) {
            // 删除OSS的数据
            String documentUrl = knowledgeBase.getDocumentUrl();
            ossAliyunFileStorageService.delete(documentUrl);

            // 删除向量库的数据
            String idsJSON = knowledgeBase.getRemark();
            List<String> idList = JSONUtil.toList(idsJSON, String.class);
            vectorStore.delete(idList);
        }

        // 删除数据库的数据
        boolean result = this.removeByIds(Arrays.asList(ids));
        return result ? 1 :0;
    }

    /**
     * 删除知识库信息
     * 
     * @param id 知识库主键
     * @return 结果
     */
    @Override
    public int deleteKnowledgeBaseById(Long id)
    {
        return removeById(id)? 1 : 0;
    }
}
