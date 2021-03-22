package com.mycommonlibrary.frame.http;

import android.annotation.SuppressLint;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import android.util.ArrayMap;
import com.google.gson.*;
import com.trello.lifecycle2.android.lifecycle.AndroidLifecycle;
import com.trello.rxlifecycle2.LifecycleProvider;
import com.trello.rxlifecycle2.LifecycleTransformer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.*;
import okio.Buffer;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Retrofit2+RxJava2
 * 导包列表：
 * // RxJava2+RxAndroid全家桶
 * implementation 'io.reactivex.rxjava2:rxandroid:2.1.0'
 * implementation 'io.reactivex.rxjava2:rxjava:2.2.4'
 * implementation 'io.reactivex.rxjava2:rxkotlin:2.3.0'
 * // OKHttp
 * implementation 'com.squareup.okhttp3:okhttp:3.12.0'
 * // retrofit2
 * implementation 'com.google.code.gson:gson:2.8.5'
 * implementation 'com.squareup.retrofit2:retrofit:2.5.0'
 * implementation 'com.squareup.retrofit2:adapter-rxjava2:2.4.0'
 * implementation 'com.squareup.retrofit2:converter-gson:2.3.0'
 * <p>
 * 通过Stetho来实现，chrome调试Android网络请求步骤
 * 1)导包
 * compile 'com.facebook.stetho:stetho:1.5.0'
 * compile 'com.facebook.stetho:stetho-okhttp3:1.5.0'
 * 2)初始化在Application的onCreate方法中添加代码Stetho.initializeWithDefaults(this);
 * 3)添加OkHttpClient.Builder().addNetworkInterceptor(new StethoInterceptor())
 */
public class RetrofitUtils {
    private static final String TAG = "RetrofitUtils:";
    private static retrofit2.Retrofit RETROFIT;
    private static final Map<Class<?>, Object> SERVICE_MAP = new ArrayMap<>();
    private static String baseUrl;

    public static void initBaseUrl(String baseUrl) {
        RetrofitUtils.baseUrl = baseUrl;
    }

    public static void build(Interceptor[] interceptors) {
        // 初始化okhttp3
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(2000, TimeUnit.MILLISECONDS)
                .readTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES);


        if (interceptors != null)
            for (Interceptor interceptor : interceptors)
                builder.addInterceptor(interceptor);
        builder.addInterceptor(new HttpLoggingInterceptor());

        OkHttpClient okHttpClient = builder.build();

        // 初始化reftrofit2
        RETROFIT = new retrofit2.Retrofit.Builder()
                .baseUrl(RetrofitUtils.baseUrl)
                .client(okHttpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder()
                        .serializeNulls()
                        .registerTypeAdapter(int.class, new GsonIntegerDefaultAdapter())
                        .create()))
                .build();
        SERVICE_MAP.clear();
    }

    public static void addHeader(final Map<String, String> map) {
        Interceptor interceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request.Builder builder = chain.request().newBuilder();
                for (String key : map.keySet())
                    builder.header(key, map.get(key));
                return chain.proceed(builder.build());
            }
        };
        build(new Interceptor[]{interceptor});
    }

    private static class GsonIntegerDefaultAdapter implements JsonSerializer<Integer>, JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                if (json.getAsString().equals("") || json.getAsString().equals("null")) {//定义为int类型,如果后台返回""或者null,则返回0
                    return 0;
                }
            } catch (Exception ignore) {
            }
            try {
                return json.getAsInt();
            } catch (NumberFormatException e) {
                throw new JsonSyntaxException(e);
            }
        }

        @Override
        public JsonElement serialize(Integer src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src);
        }
    }

    private static String convertRequestBodyToString(RequestBody request) throws IOException {
        Buffer buffer = new Buffer();
        request.writeTo(buffer);
        return buffer.readUtf8();
    }

    /**
     * 调用入口
     */
    @SuppressLint("CheckResult")
    public static <T> T create(Class<T> service) {
        if (RETROFIT == null)
            build(null);
        Object o = SERVICE_MAP.get(service);
        if (o != null) {
            return (T) o;
        } else {
            T t = RETROFIT.create(service);
            SERVICE_MAP.put(service, t);
            return t;
        }
    }

    public static <T> LifecycleTransformer<T> bindLifecycle(LifecycleOwner owner) {
        LifecycleProvider<Lifecycle.Event> provider = AndroidLifecycle.createLifecycleProvider(owner);
        return provider.bindToLifecycle();
    }
}