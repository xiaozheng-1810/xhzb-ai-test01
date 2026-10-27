package com.xhzb.config;

import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

@Configuration
public class SpringAIConfig {

    @Bean
    public VectorStore redisVectorStore(OpenAiEmbeddingModel openAiEmbeddingModel) {
        JedisPooled jedisPooled = new JedisPooled("192.168.100.168",6378);
        return RedisVectorStore.builder(jedisPooled, openAiEmbeddingModel)
                .indexName("spring-ai-index") //向量索引名称
                .prefix("doc:") //向量索引名称前缀
                .initializeSchema(true) //应用启动自动创建索引，首次启动true；生产关闭自动建索引false
                .build();
    }
}
