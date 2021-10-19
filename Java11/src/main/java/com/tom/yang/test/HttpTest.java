package com.tom.yang.test;

import org.junit.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * @author qk965
 * @date 2021-07-15 12:14
 */
public class HttpTest {

    @Test
    public void test()throws Exception{
        var request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.baidu.com/"))
                .build();
        var client = HttpClient.newHttpClient();

        // 同步
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());

        // 异步
        CompletableFuture<HttpResponse<String>> async = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        // 调用 get 会阻塞
        HttpResponse<String> response2 = async.get();
        System.out.println(response2);
    }
}
