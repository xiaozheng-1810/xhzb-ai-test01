package com.xhzb;

import com.google.gson.Gson;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OkHttpLlmSyncDemo {
    private static final String API_URL = "https://api.minimax.cn/v1/chat/completions";
    private static  String API_KEY ;
    private static final Gson GSON = new Gson();

    // 全局单例OkHttpClient！不要每次new，防止连接泄漏
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    public static void main(String[] args) throws IOException {
        API_KEY =  System.getenv().get("ANTHROPIC_AUTH_TOKEN_TEMP"); //从环境变量中读取API KEY

        Map<String, Object> reqBody = Map.of(
                "model", "minimax-m3",
                "messages", List.of(
                        Map.of("role", "user", "content", "用介绍一下鲁迅的平生")
                ),
                "temperature", 0.7,
                "stream", false
        );

        RequestBody body = RequestBody.create(
                MediaType.get("application/json; charset=utf-8"),
                GSON.toJson(reqBody)
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        // try-with-resources 自动关闭response，防止句柄泄露
        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.out.println("请求失败，code=" + response.code());
                return;
            }
            String respStr = response.body().string();
            System.out.println("返回结果：\n" + respStr);
        }
    }
}