package com.dusk.module.auth.utils.okhttp;

import com.dusk.common.core.exception.BusinessException;
import okhttp3.*;
import okhttp3.MediaType;
import okio.Buffer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link OkHttpTask} 单元测试，目标：>=95% 分支覆盖。
 *
 * <p>生产代码把 {@link OkHttpClient} 存在 private static final 字段里，测试通过
 * {@code sun.misc.Unsafe} 替换该字段以注入 mock 客户端，整个用例不会发起真实网络请求。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OkHttpTaskTest {

    private static Object originalClient;

    @Mock
    private Call call;
    @Mock
    private Response response;
    @Mock
    private ResponseBody responseBody;

    private OkHttpClient client;
    private OkHttpTask task;

    @BeforeAll
    static void captureOriginalClient() throws Exception {
        Class.forName(OkHttpTask.class.getName());
        originalClient = readStaticClient();
    }

    @AfterAll
    static void restoreOriginalClient() throws Exception {
        writeStaticClient(originalClient);
    }

    private static Object unsafe() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return field.get(null);
    }

    // ------------------------------------------------------------------
    // reflection helpers：替换 OkHttpTask 的 private static final client
    // ------------------------------------------------------------------

    private static Object staticFieldBase() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        return unsafeClass.getMethod("staticFieldBase", Field.class)
                .invoke(unsafe(), OkHttpTask.class.getDeclaredField("client"));
    }

    private static long staticFieldOffset() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        return (long) unsafeClass.getMethod("staticFieldOffset", Field.class)
                .invoke(unsafe(), OkHttpTask.class.getDeclaredField("client"));
    }

    private static Object readStaticClient() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        java.lang.reflect.Method getObject = unsafeClass.getMethod("getObject", Object.class, long.class);
        return getObject.invoke(unsafe(), staticFieldBase(), staticFieldOffset());
    }

    private static void writeStaticClient(Object value) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        java.lang.reflect.Method putObject = unsafeClass.getMethod("putObject", Object.class, long.class, Object.class);
        putObject.invoke(unsafe(), staticFieldBase(), staticFieldOffset(), value);
    }

    private static OkHttpTaskParam param(String url, Method method) {
        Map<String, String> header = new LinkedHashMap<>();
        header.put("X-Token", "token-value");
        return new OkHttpTaskParam(url, method, null, null, header, null);
    }

    private static String bodyOf(Request request) throws IOException {
        RequestBody body = request.body();
        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        return buffer.readUtf8();
    }

    @BeforeEach
    void setUp() throws Exception {
        client = mock(OkHttpClient.class);
        writeStaticClient(client);
        task = new OkHttpTask();
        when(client.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.string()).thenReturn("ok");
    }

    // ------------------------------------------------------------------
    // dataTask
    // ------------------------------------------------------------------

    @Test
    @DisplayName("dataTask：GET 请求把 query 拼到 URL 上并返回响应体")
    void dataTask_getWithQuery() throws Exception {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("a", "b");
        query.put("c", "d");
        OkHttpTaskParam param = param("http://example.com/api", Method.GET);
        param.setQuery(query);

        String result = task.dataTask(param);

        assertThat(result).isEqualTo("ok");
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(client).newCall(captor.capture());
        Request request = captor.getValue();
        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.url().toString()).contains("?a=b&").contains("c=d");
        assertThat(request.header("X-Token")).isEqualTo("token-value");
    }

    @Test
    @DisplayName("dataTask：GET URL 已含 ? 时不再追加问号")
    void dataTask_getWithExistingQuestionMark() throws Exception {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("a", "b");
        OkHttpTaskParam param = param("http://example.com/api?x=1", Method.GET);
        param.setQuery(query);

        task.dataTask(param);

        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(client).newCall(captor.capture());
        assertThat(captor.getValue().url().toString()).isEqualTo("http://example.com/api?x=1a=b&");
    }

    @Test
    @DisplayName("dataTask：GET 无 query 时保持原始 URL")
    void dataTask_getWithoutQuery() throws Exception {
        task.dataTask(param("http://example.com/api", Method.GET));

        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(client).newCall(captor.capture());
        assertThat(captor.getValue().url().toString()).isEqualTo("http://example.com/api");
    }

    @Test
    @DisplayName("dataTask：GET 空 query 时保持原始 URL")
    void dataTask_getWithEmptyQuery() throws Exception {
        OkHttpTaskParam param = param("http://example.com/api", Method.GET);
        param.setQuery(Map.of());

        task.dataTask(param);

        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(client).newCall(captor.capture());
        assertThat(captor.getValue().url().toString()).isEqualTo("http://example.com/api");
    }

    @Test
    @DisplayName("dataTask：POST 有 body 时发送 JSON")
    void dataTask_postWithJsonBody() throws Exception {
        OkHttpTaskParam param = param("http://example.com/api", Method.POST);
        param.setBody("{\"name\":\"dusk\"}");

        task.dataTask(param);

        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(client).newCall(captor.capture());
        Request request = captor.getValue();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.body().contentType().toString()).isEqualTo("application/json; charset=utf-8");
        assertThat(bodyOf(request)).isEqualTo("{\"name\":\"dusk\"}");
    }

    @Test
    @DisplayName("dataTask：POST 无 body 时把 query 作为表单发送")
    void dataTask_postWithFormBody() throws Exception {
        OkHttpTaskParam param = param("http://example.com/api", Method.POST);
        Map<String, String> query = new LinkedHashMap<>();
        query.put("k", "v");
        param.setQuery(query);

        task.dataTask(param);

        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(client).newCall(captor.capture());
        Request request = captor.getValue();
        assertThat(request.body().contentType().toString()).startsWith("application/x-www-form-urlencoded");
        assertThat(bodyOf(request)).isEqualTo("k=v");
    }

    @Test
    @DisplayName("dataTask：POST 无 body 无 query 时发送空表单")
    void dataTask_postWithoutBodyAndQuery() throws Exception {
        task.dataTask(param("http://example.com/api", Method.POST));

        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(client).newCall(captor.capture());
        assertThat(bodyOf(captor.getValue())).isEmpty();
    }

    // ------------------------------------------------------------------
    // validateResponse
    // ------------------------------------------------------------------

    @Test
    @DisplayName("dataTask：HTTP 非 200 时抛出携带响应体的 BusinessException")
    void dataTask_non200ThrowsBusinessException() throws Exception {
        when(response.isSuccessful()).thenReturn(false);
        when(response.code()).thenReturn(500);
        when(responseBody.string()).thenReturn("server error");

        assertThatThrownBy(() -> task.dataTask(param("http://example.com/api", Method.GET)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("server error");
    }

    @Test
    @DisplayName("dataTask：读取错误响应体失败时抛出统一的 BusinessException")
    void dataTask_bodyReadFails() throws Exception {
        when(response.isSuccessful()).thenReturn(false);
        when(response.code()).thenReturn(500);
        when(responseBody.string()).thenThrow(new IOException("read failed"));

        assertThatThrownBy(() -> task.dataTask(param("http://example.com/api", Method.GET)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("http请求调用异常");
    }

    @Test
    @DisplayName("dataTask：isSuccessful 为 false 但 code 为 200 时不抛异常")
    void dataTask_notSuccessfulBut200() throws Exception {
        when(response.isSuccessful()).thenReturn(false);
        when(response.code()).thenReturn(200);

        assertThat(task.dataTask(param("http://example.com/api", Method.GET))).isEqualTo("ok");
    }

    @Test
    @DisplayName("dataTask：请求超时时原样抛出 SocketTimeoutException")
    void dataTask_timeout() throws Exception {
        when(call.execute()).thenThrow(new SocketTimeoutException("timeout"));

        assertThatThrownBy(() -> task.dataTask(param("http://example.com/api", Method.GET)))
                .isInstanceOf(SocketTimeoutException.class)
                .hasMessage("timeout");
    }

    // ------------------------------------------------------------------
    // formTask
    // ------------------------------------------------------------------

    @Test
    @DisplayName("formTask：query 作为 multipart 表单字段发送")
    void formTask_withQuery() throws Exception {
        OkHttpTaskParam param = param("http://example.com/upload", Method.POST);
        Map<String, String> query = new LinkedHashMap<>();
        query.put("name", "dusk");
        param.setQuery(query);

        String result = task.formTask(param);

        assertThat(result).isEqualTo("ok");
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(client).newCall(captor.capture());
        Request request = captor.getValue();
        assertThat(request.body()).isInstanceOf(MultipartBody.class);
        assertThat(bodyOf(request)).contains("name=\"name\"").contains("dusk");
    }

    @Test
    @DisplayName("formTask：没有表单字段时 OkHttp 拒绝构建空 multipart")
    void formTask_withoutQuery() {
        OkHttpTaskParam param = param("http://example.com/upload", Method.POST);

        assertThatThrownBy(() -> task.formTask(param))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("formTask：HTTP 失败时抛出 BusinessException")
    void formTask_httpFailure() throws Exception {
        when(response.isSuccessful()).thenReturn(false);
        when(response.code()).thenReturn(503);
        when(responseBody.string()).thenReturn("unavailable");
        OkHttpTaskParam param = param("http://example.com/upload", Method.POST);
        param.setQuery(Map.of("k", "v"));

        assertThatThrownBy(() -> task.formTask(param))
                .isInstanceOf(BusinessException.class)
                .hasMessage("unavailable");
    }

    @Test
    @DisplayName("OkHttpTask.JSON：内容类型为 JSON UTF-8")
    void jsonMediaType() {
        MediaType json = OkHttpTask.JSON;
        assertThat(json).isNotNull();
        assertThat(json.toString()).isEqualTo("application/json; charset=utf-8");
    }
}
