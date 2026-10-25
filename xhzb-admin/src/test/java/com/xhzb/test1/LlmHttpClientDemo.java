package com.xhzb.test1;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
    // 匹配markdown代码块 ```java ... ```
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(\\w+)?\\s*\\n([\\s\\S]*?)```");

    public static void main(String[] args) {
        API_KEY = System.getenv().get("ANTHROPIC_AUTH_TOKEN_TEMP");
        if (API_KEY == null || API_KEY.isBlank()) {
            System.err.println("\n[ERROR] 环境变量 ANTHROPIC_AUTH_TOKEN_TEMP 未配置！");
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

            System.out.println("\n==================== 接口响应信息 ====================");
            System.out.printf("响应状态码: %d%n", code);
            System.out.println("======================================================");

            if (code != 200) {
                System.err.println("\n[ERROR] 请求失败，原始返回：");
                System.out.println(respBody);
                return;
            }

            // 解析json拿到回答内容
            JsonObject root = JsonParser.parseString(respBody).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            String content = choices.get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content")
                    .getAsString();

            System.out.println("\n==================== LLM 返回内容 ====================");
            printPrettyForIdeaConsole(content);
            System.out.println("======================================================\n");

        } catch (Exception e) {
            System.err.println("\n[ERROR] 请求异常：");
            e.printStackTrace();
        }
    }

    /**
     * 专门给 IDEA 控制台美化打印
     * 普通文字正常输出，代码块加上边框标记区分
     */
    private static void printPrettyForIdeaConsole(String text) {
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(text);
        int last = 0;
        while (matcher.find()) {
            // 打印代码块前面普通文本
            if (matcher.start() > last) {
                System.out.print(text.substring(last, matcher.start()));
            }
            String lang = matcher.group(1);
            String code = matcher.group(2);
            System.out.printf("%n+----------------【%s 代码开始】----------------+%n", lang == null ? "code" : lang);
            System.out.println(code);
            System.out.println("+----------------【代码结束】----------------+");
            last = matcher.end();
        }
        // 剩余尾部文字
        if (last < text.length()) {
            System.out.print(text.substring(last));
        }
    }
}
