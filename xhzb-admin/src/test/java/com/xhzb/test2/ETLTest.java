package com.xhzb.test2;

import org.junit.jupiter.api.Test;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.InputStreamResource;

import org.springframework.ai.document.Document;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

@SpringBootTest
public class ETLTest {
    @Autowired
    private VectorStore vectorStore;

    @Test
    public void testPDFByParagraph() throws FileNotFoundException {
        InputStreamResource inputStreamResource = new InputStreamResource(new FileInputStream("D:\\AI应用与智能体开发学习资料\\黑马资料\\AI工程化基础2\\星海智伴\\day03-大模型应用基础(SpringAI)\\资料\\护理员工工作手册.pdf"));

        ParagraphPdfDocumentReader paragraphPdfDocumentReader = new ParagraphPdfDocumentReader(inputStreamResource,
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0) // 设置页眉边距
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0) // 删除页眉顶部的文本行数
                                .build())
                        .withPagesPerDocument(1) // 每个文档的页数
                        .build());

        System.out.println(paragraphPdfDocumentReader.read());
    }

    @Test
    public void testTestSplitter() throws FileNotFoundException {

        // 读取文件
        // 把文件转为InputStreamResource输入流
        InputStreamResource resource = new InputStreamResource(new FileInputStream("D:\\AI应用与智能体开发学习资料\\黑马资料\\AI工程化基础2\\星海智伴\\day03-大模型应用基础(SpringAI)\\资料\\护理员工工作手册.pdf"));

        // PagePdfDocumentReader里面需要传递输入流和配置
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource,
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0) // 设置页眉边距
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0) // 删除页眉顶部的文本行数
                                .build())
                        .withPagesPerDocument(1) // 每个文档的页数
                        .build());

        // 创建TextSplitter
        // 创建TokenTextSplitter,按token(这个Token指的是模型理解的最小单位)进行切分
        TextSplitter textSplitter = new TokenTextSplitter();
        System.out.println("分隔之前的文档数："+pdfReader.read().size());
        // apply()就是执行切片转换，返回新的文档列表
        List<Document> documents = textSplitter.apply(pdfReader.read());
        System.out.println("分隔之后的文档数："+documents.size());
        System.out.println(documents);

        //存储到向量数据库中
        vectorStore.add(documents);
    }

    @Test
    public void search() {
        SearchRequest searchRequest = SearchRequest.builder()
                .query("护理服务宗旨与核心价值是什么")
                .similarityThreshold(0.5)
                .topK(1)
                .build();
        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        for (Document document : documents) {
            System.out.println(document);
        }
    }
}
