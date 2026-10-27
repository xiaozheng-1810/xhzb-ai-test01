package com.xhzb.test2;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;


@SpringBootTest
public class RedisVectorTest {

    @Autowired
    private VectorStore vectorStore;
    
    @Test
    public void addDocument(){
        List<Document> docs = List.of(
                new Document("Redis Stack内置RediSearch向量检索，可用于RAG系统",
                        Map.of("category", "db", "type", "vector")),
                new Document("Spring AI封装统一VectorStore接口，底层支持Redis、Milvus等向量库",
                        Map.of("category", "framework", "type", "ai")),
                new Document("RAG检索增强生成：先向量召回上下文，再送入大模型生成答案",
                        Map.of("category", "llm", "type", "rag"))
        );
        vectorStore.add(docs);
    }
}