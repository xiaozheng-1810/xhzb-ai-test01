package com.xhzb.test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LlmHttpClientDemo {
    private static final String API_URL = "https://api.minimax.cn/v1/chat/completions";
    private static String API_KEY;
    // 提取content的正则，适配MiniMax OpenAI兼容返回
    private static final Pattern CONTENT_PATTERN = Pattern.compile("\"content\"\\s*:\\s*\"((?:\\\\.|[^\"])*?)\"");
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(\\w+)?\\s*\\n([\\s\\S]*?)```");

    public static void main(String[] args) {
        API_KEY = System.getenv().get("ANTHROPIC_AUTH_TOKEN_TEMP");
        if (API_KEY == null || API_KEY.isBlank()) {
            System.err.println("❌ 错误：环境变量 ANTHROPIC_AUTH_TOKEN_TEMP 未配置！");
            return;
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        String requestBody = """
                {
                    "model": "MiniMax-M3",
                    "messages": [
                        {"role": "user", "content": "用Java写一个冒泡排序"}
                    ],
                    "temperature": 0.7
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .timeout(java.time.Duration.ofSeconds(60))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            String respBody = response.body();

            System.out.println("========================================");
            System.out.println("响应状态码：" + code);
            System.out.println("========================================");

            if (code != 200) {
                System.err.println("❌ 请求失败，原始返回：");
                System.out.println(respBody);
                return;
            }

            String llmContent = extractContent(respBody);
            if (llmContent == null) {
                System.err.println("⚠️ 未能提取模型回答，原始报文：");
                System.out.println(respBody);
                return;
            }

            System.out.println("✅ 模型回答（美化渲染）：");
            System.out.println("----------------------------------------");
            prettyPrintMarkdown(llmContent);
            System.out.println("----------------------------------------");

        } catch (Exception e) {
            System.err.println("❌ 请求发生异常：");
            e.printStackTrace();
        }
    }

    /**
     * 正则提取content，并转义还原 \n \\"
     */
    private static String extractContent(String json) {
        Matcher m = CONTENT_PATTERN.matcher(json);
        if (m.find()) {
            String raw = m.group(1);
            return raw
                    .replace("\\\\n", "\n")
                    .replace("\\\\\"", "\"")
                    .replace("\\\\t", "\t");
        }
        return null;
    }

    /**
     * markdown美化，分割代码块
     */
    private static void prettyPrintMarkdown(String text) {
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(text);
        int lastIndex = 0;
        while (matcher.find()) {
            if (matcher.start() > lastIndex) {
                System.out.print(text.substring(lastIndex, matcher.start()));
            }
            String lang = matcher.group(1);
            String code = matcher.group(2);
            System.out.printf("\n=====【%s 代码块开始】=====\n", lang == null ? "" : lang);
            System.out.println(code);
            System.out.println("=====【代码块结束】=====\n");
            lastIndex = matcher.end();
        }
        if (lastIndex < text.length()) {
            System.out.print(text.substring(lastIndex));
        }
    }
}
