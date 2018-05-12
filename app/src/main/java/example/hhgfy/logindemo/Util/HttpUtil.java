package example.hhgfy.logindemo.Util;

/**
 * Created by hhgfy on 2018/5/11.
 */


import org.json.JSONObject;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpUtil {
    //封装OkHttp请求
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();

    //Get
    public String get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();

        return response.body().string();
    }


    //Post
    public String post(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();

    }

    public String post(String url, FormBody formBody) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();

    }

}
