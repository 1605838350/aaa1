package com.ruoyi.common.utils.webdav;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.apache.hc.core5.util.Timeout;

import com.ruoyi.common.storage.FileStorageStrategy;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * WebDAV 方法：MKCOL
 */
class HttpMkcol extends HttpUriRequestBase {
    public HttpMkcol(String uri) {
        super("MKCOL", URI.create(uri));
    }

    @Override
    public String getMethod() {
        return "MKCOL";
    }
}

/**
 * WebDAV 方法：MOVE
 */
class HttpMove extends HttpUriRequestBase {
    public HttpMove(String uri) {
        super("MOVE", URI.create(uri));
    }

    @Override
    public String getMethod() {
        return "MOVE";
    }
}

/**
 * WebDAV 方法：PROPFIND
 */
class HttpPropfind extends HttpUriRequestBase {
    public HttpPropfind(String uri) {
        super("PROPFIND", URI.create(uri));
    }

    @Override
    public String getMethod() {
        return "PROPFIND";
    }
}

public class WebDavClient {

    private final String baseUrl;
    private final String username;
    private final String password;
    private final int connectTimeoutMs;
    private final int connectionRequestTimeoutMs;
    private final int responseTimeoutMs;
    private final int maxConnTotal;
    private final int maxConnPerRoute;
    private final CloseableHttpClient httpClient;

    public WebDavClient(String baseUrl, String username, String password) {
        this(baseUrl, username, password, 10000, 30000, 600000, 200, 50);
    }

    public WebDavClient(String baseUrl, String username, String password,
                        int connectTimeoutMs,
                        int connectionRequestTimeoutMs,
                        int responseTimeoutMs,
                        int maxConnTotal,
                        int maxConnPerRoute) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.username = username;
        this.password = password;
        this.connectTimeoutMs = connectTimeoutMs;
        this.connectionRequestTimeoutMs = connectionRequestTimeoutMs;
        this.responseTimeoutMs = responseTimeoutMs;
        this.maxConnTotal = maxConnTotal;
        this.maxConnPerRoute = maxConnPerRoute;
        this.httpClient = createHttpClient();
    }

    /**
     * 创建支持自签名证书的 HttpClient
     */
    private CloseableHttpClient createHttpClient() {
        try {
            // 信任所有证书
            TrustStrategy trustStrategy = (X509Certificate[] chain, String authType) -> true;
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, trustStrategy)
                    .build();

            SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                    .setSslContext(sslContext)
                    .setHostnameVerifier((hostname, session) -> true)
                    .build();

            PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(sslSocketFactory)
                    .setMaxConnTotal(maxConnTotal)
                    .setMaxConnPerRoute(maxConnPerRoute)
                    .build();

                RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
                    .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectionRequestTimeoutMs))
                    .setResponseTimeout(Timeout.ofMilliseconds(responseTimeoutMs))
                    .build();

            return HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(requestConfig)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("创建 HttpClient 失败", e);
        }
    }

    /**
     * 获取 Basic Auth 头
     */
    private String getAuthorization() {
        String auth = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 构建完整 URL（对路径中的中文等特殊字符进行编码）
     */
    private String buildUrl(String path) {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        
        // 对路径的每一部分进行 URL 编码，保留 / 分隔符
        String[] parts = cleanPath.split("/");
        StringBuilder encodedPath = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                encodedPath.append("/");
            }
            if (!parts[i].isEmpty()) {
                try {
                    // 对每个路径部分进行 URL 编码，空格编码为 %20 而不是 +
                    String encoded = java.net.URLEncoder.encode(parts[i], "UTF-8");
                    // 将 + 替换为 %20，因为 URL 中空格的规范编码是 %20
                    encoded = encoded.replace("+", "%20");
                    encodedPath.append(encoded);
                } catch (java.io.UnsupportedEncodingException e) {
                    encodedPath.append(parts[i]);
                }
            }
        }
        
        return baseUrl + "/" + encodedPath.toString();
    }

    /**
     * 检查文件或目录是否存在
     */
    public boolean exists(String path) throws IOException {
        String url = buildUrl(path);

        HttpPropfind request = new HttpPropfind(url);
        request.setHeader("Authorization", getAuthorization());
        request.setHeader("Depth", "0");
        request.setEntity(new StringEntity(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                        "<D:propfind xmlns:D=\"DAV:\">" +
                        "<D:prop><D:resourcetype/></D:prop>" +
                        "</D:propfind>",
                ContentType.APPLICATION_XML));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int code = response.getCode();
            return code == 207 || code == 200;
        }
    }

    /**
     * 上传文件
     */
    public void upload(String path, InputStream inputStream, long contentLength) throws IOException {
        String url = buildUrl(path);

        HttpPut request = new HttpPut(url);
        request.setHeader("Authorization", getAuthorization());
        request.setEntity(new InputStreamEntity(inputStream, contentLength, ContentType.APPLICATION_OCTET_STREAM));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int code = response.getCode();

            if (code != 201 && code != 200 && code != 204) {
                String body = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
                throw new IOException("上传失败，HTTP 状态码：" + code + ", 响应：" + body);
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 下载文件
     */
    public InputStream download(String path) throws IOException {
        String url = buildUrl(path);

        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", getAuthorization());

        CloseableHttpResponse response = httpClient.execute(request);
        int code = response.getCode();
        if (code != 200) {
            response.close();
            throw new IOException("下载失败，HTTP 状态码：" + code);
        }

        HttpEntity entity = response.getEntity();
        // 包装 InputStream，关闭时同时关闭 response
        return new FilterInputStream(entity.getContent()) {
            @Override
            public void close() throws IOException {
                super.close();
                response.close();
            }
        };
    }

    /**
     * 删除文件或目录
     */
    public void delete(String path) throws IOException {
        String url = buildUrl(path);

        HttpDelete request = new HttpDelete(url);
        request.setHeader("Authorization", getAuthorization());

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int code = response.getCode();
            if (code != 200 && code != 204) {
                throw new IOException("删除失败，HTTP 状态码：" + code);
            }
        }
    }

    /**
     * 创建目录（递归创建多级目录）
     */
    public void mkdirs(String path) throws IOException {
        // 规范化路径
        String normalizedPath = path.trim();
        if (normalizedPath.isEmpty()) {
            return;
        }

        // 去除末尾的 /
        if (normalizedPath.length() > 1 && normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }

        // 逐级创建目录
        String[] parts = normalizedPath.split("/");
        StringBuilder currentPath = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            if (currentPath.length() == 0) {
                currentPath.append("/").append(part);
            } else {
                currentPath.append("/").append(part);
            }

            String pathToCreate = currentPath.toString();
            String url = buildUrl(pathToCreate);

            HttpMkcol request = new HttpMkcol(url);
            request.setHeader("Authorization", getAuthorization());

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int code = response.getCode();

                // 201 = 创建成功
                if (code == 201) {
                    continue;
                }
                // 200/405 = 目录已存在
                else if (code == 200 || code == 405) {
                    continue;
                }
                // 409 = 父目录不存在
                else if (code == 409) {
                    continue;
                }
                // 401/403 = 权限问题
                else if (code == 401 || code == 403) {
                    throw new IOException("创建目录权限不足，HTTP 状态码：" + code);
                } else {
                    throw new IOException("创建目录失败，HTTP 状态码：" + code);
                }
            }
        }
    }

    /**
     * 移动/重命名文件或目录
     * @param oldPath 原路径
     * @param newPath 新路径
     */
    public void move(String oldPath, String newPath) throws IOException {
        String oldUrl = buildUrl(oldPath);
        String newUrl = buildUrl(newPath);
        
        HttpMove request = new HttpMove(oldUrl);
        request.setHeader("Authorization", getAuthorization());
        request.setHeader("Destination", newUrl);
        request.setHeader("Overwrite", "F"); // F = 不覆盖已存在的目标
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int code = response.getCode();
            if (code != 201 && code != 204) {
                throw new IOException("移动失败，HTTP 状态码：" + code);
            }
        }
    }

    /**
     * 列出目录下的文件和子目录
     */
    public List<FileStorageStrategy.FileInfo> listFiles(String path) throws IOException {
        String url = buildUrl(path);
        HttpPropfind request = new HttpPropfind(url);
        request.setHeader("Authorization", getAuthorization());
        request.setHeader("Depth", "1"); // 只列出当前目录的内容
        
        // PROPFIND 请求体
        String propfindBody = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<D:propfind xmlns:D=\"DAV:\">" +
            "<D:prop>" +
            "<D:displayname/>" +
            "<D:resourcetype/>" +
            "<D:getcontentlength/>" +
            "<D:getlastmodified/>" +
            "</D:prop>" +
            "</D:propfind>";
        request.setEntity(new StringEntity(propfindBody, ContentType.APPLICATION_XML));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int code = response.getCode();
            if (code != 207) { // 207 Multi-Status
                throw new IOException("列出目录失败，HTTP 状态码：" + code);
            }
            
            String responseBody;
            try {
                responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            } catch (ParseException e) {
                throw new IOException("解析响应失败：" + e.getMessage(), e);
            }
            return parsePropfindResponse(responseBody, path);
        }
    }
    
    /**
     * 解析 PROPFIND 响应
     */
    private List<FileStorageStrategy.FileInfo> parsePropfindResponse(String responseBody, String parentPath) {
        List<FileStorageStrategy.FileInfo> files = new ArrayList<>();
        
        //System.out.println("[DEBUG] WebDAV Response: " + responseBody.substring(0, Math.min(2000, responseBody.length())));
        
        // 简单的 XML 解析（提取 href 和属性）
        // 支持大小写的 response 标签
        String[] responses = responseBody.split("<[Dd]:response>|<response>");
        
        for (int i = 1; i < responses.length; i++) { // 跳过第一个空元素
            String resp = responses[i];
            
            // 提取 href（文件/目录路径）
            String href = extractXmlValue(resp, "D:href");
            if (href == null) {
                // 尝试不带命名空间的方式
                href = extractXmlValue(resp, "href");
            }
            if (href == null) continue;
            
            // 提取文件名（从 href 中）
            String name = href;
            if (name.endsWith("/")) {
                name = name.substring(0, name.length() - 1);
            }
            int lastSlash = name.lastIndexOf('/');
            if (lastSlash >= 0) {
                name = name.substring(lastSlash + 1);
            }
            
            // URL 解码中文文件名
            try {
                name = java.net.URLDecoder.decode(name, "UTF-8");
            } catch (Exception e) {
                // 解码失败保持原样
            }
            
            // 跳过当前目录本身
            // parentPath 可能是相对路径（如 "test/aaa"），需要处理各种情况
            String normalizedParentPath = parentPath;
            if (!normalizedParentPath.startsWith("/")) {
                normalizedParentPath = "/" + normalizedParentPath;
            }
            if (normalizedParentPath.endsWith("/")) {
                normalizedParentPath = normalizedParentPath.substring(0, normalizedParentPath.length() - 1);
            }
            // 提取 parentPath 的最后一部分（目录名）
            String parentDirName = normalizedParentPath;
            int lastSlashIdx = normalizedParentPath.lastIndexOf('/');
            if (lastSlashIdx >= 0) {
                parentDirName = normalizedParentPath.substring(lastSlashIdx + 1);
            }
            
            if (name.isEmpty() || name.equals(parentPath) || name.equals(normalizedParentPath) 
                    || name.equals(parentDirName) || name.equals("/")) {
                continue;
            }
            
            // 检查是否是目录 - 多种格式兼容
            boolean isDirectory = resp.contains("<D:collection/>") 
                || resp.contains("<D:collection />")
                || resp.contains("<d:collection/>")
                || resp.contains("<d:collection />")
                || (resp.contains("<D:resourcetype>") && resp.contains("collection"))
                || (resp.contains("<d:resourcetype>") && resp.contains("collection"))
                || href.endsWith("/"); // href 以 / 结尾通常是目录
            
            //System.out.println("[DEBUG] Parsed: name=" + name + ", href=" + href + ", isDirectory=" + isDirectory);
            
            // 提取文件大小
            String sizeStr = extractXmlValue(resp, "D:getcontentlength");
            long size = 0;
            try {
                size = sizeStr != null ? Long.parseLong(sizeStr) : 0;
            } catch (NumberFormatException e) {
                size = 0;
            }
            
            // 提取修改时间
            String modifiedStr = extractXmlValue(resp, "D:getlastmodified");
            long lastModified = 0;
            if (modifiedStr != null) {
                try {
                    // 尝试解析 RFC 1123 日期格式
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US);
                    lastModified = sdf.parse(modifiedStr).getTime();
                } catch (Exception e) {
                    lastModified = 0;
                }
            }
            
            // 构建文件路径
            String filePath = parentPath;
            if (!filePath.endsWith("/")) {
                filePath += "/";
            }
            filePath += name;
            
            FileStorageStrategy.FileInfo info = new FileStorageStrategy.FileInfo();
            info.setName(name);
            info.setPath(filePath);
            info.setDirectory(isDirectory);
            info.setSize(size);
            info.setLastModified(lastModified);
            files.add(info);
        }
        
        return files;
    }
    
    /**
     * 从 XML 中提取指定标签的值（支持大小写）
     */
    private String extractXmlValue(String xml, String tagName) {
        // 尝试原始标签
        String startTag = "<" + tagName + ">";
        String endTag = "</" + tagName + ">";
        int start = xml.indexOf(startTag);
        int end = xml.indexOf(endTag);
        if (start >= 0 && end > start) {
            return xml.substring(start + startTag.length(), end);
        }
        
        // 尝试小写版本
        String lowerTagName = tagName.toLowerCase();
        startTag = "<" + lowerTagName + ">";
        endTag = "</" + lowerTagName + ">";
        start = xml.indexOf(startTag);
        end = xml.indexOf(endTag);
        if (start >= 0 && end > start) {
            return xml.substring(start + startTag.length(), end);
        }
        
        return null;
    }

    /**
     * 关闭客户端
     */
    public void close() throws IOException {
        httpClient.close();
    }
}
