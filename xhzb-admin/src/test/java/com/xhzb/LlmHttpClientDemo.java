package com.xhzb;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class LlmHttpClientDemo {

    // 改成你的大模型接口地址，兼容 OpenAI 格式
    //private static final String API_URL = "https://api.minimax.cn/v1/chat/completions";
    // 标准接口地址去掉/chat/completions不行
    private static final String API_URL = "https://ws-vrngnjf9izwfyzrq.cn-beijing.maas.aliyuncs.com/compatible-mode/v1/chat/completions";
    // 替换为你的key
    private static String API_KEY ;

    public static void main(String[] args) {

//        API_KEY =  System.getenv().get("ANTHROPIC_AUTH_TOKEN_TEMP"); //从环境变量中读取API KEY
        API_KEY =  System.getenv().get("BAI_LIAN_API_KEY"); //从环境变量中读取API KEY
        // 1. 创建HttpClient
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        // 2. 构造请求体 JSON
        String requestBody = """
                {
                    "model": "qwen3.8-27b",
                    "messages": [
                        {"role": "user", "content": "用Java写一个循环10次打印我爱梅军建"}
                    ],
                    "temperature": 0.7
                }
                """;

        // 3. 构建HttpRequest
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .timeout(java.time.Duration.ofSeconds(60))
                .build();

        try {
            // 4. 同步发送请求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("响应码：" + response.statusCode());
            System.out.println("响应内容：");
            System.out.println(response.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}