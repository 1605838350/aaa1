package com.ruoyi.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 读取项目相关配置
 * 
 * @author ruoyi
 */
@Component
@ConfigurationProperties(prefix = "ruoyi")
public class RuoYiConfig
{
    /** 项目名称 */
    private String name;

    /** 版本 */
    private String version;

    /** 版权年份 */
    private String copyrightYear;

    /** 上传路径 */
    private static String profile;

    /** 获取地址开关 */
    private static boolean addressEnabled;

    /** 验证码类型 */
    private static String captchaType;


    /** WebDAV 基础 URL */
    private static String webdavUrl;

    /** WebDAV 用户名 */
    private static String webdavUsername;

    /** WebDAV 密码 */
    private static String webdavPassword;

    /** WebDAV 连接超时（毫秒） */
    private static Integer webdavConnectTimeoutMs;

    /** WebDAV 连接池等待超时（毫秒） */
    private static Integer webdavConnectionRequestTimeoutMs;

    /** WebDAV 响应超时（毫秒） */
    private static Integer webdavResponseTimeoutMs;

    /** WebDAV 连接池最大连接数 */
    private static Integer webdavMaxConnTotal;

    /** WebDAV 单路由最大连接数 */
    private static Integer webdavMaxConnPerRoute;


    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getCopyrightYear()
    {
        return copyrightYear;
    }

    public void setCopyrightYear(String copyrightYear)
    {
        this.copyrightYear = copyrightYear;
    }

    public static String getProfile()
    {
        return profile;
    }

    public void setProfile(String profile)
    {
        RuoYiConfig.profile = profile;
    }

    public static boolean isAddressEnabled()
    {
        return addressEnabled;
    }

    public void setAddressEnabled(boolean addressEnabled)
    {
        RuoYiConfig.addressEnabled = addressEnabled;
    }

    public static String getCaptchaType() {
        return captchaType;
    }

    public void setCaptchaType(String captchaType) {
        RuoYiConfig.captchaType = captchaType;
    }

    /**
     * 获取导入上传路径
     */
    public static String getImportPath()
    {
        return getProfile() + "/import";
    }

    /**
     * 获取头像上传路径
     */
    public static String getAvatarPath()
    {
        return getProfile() + "/avatar";
    }

    /**
     * 获取下载路径
     */
    public static String getDownloadPath()
    {
        return getProfile() + "/download/";
    }

    /**
     * 获取上传路径
     */
    public static String getUploadPath()
    {
        return getProfile() + "/upload";
    }




    public static String getWebdavUrl() {
        return webdavUrl;
    }

    public void setWebdavUrl(String webdavUrl) {
        RuoYiConfig.webdavUrl = webdavUrl;
    }

    public static String getWebdavUsername() {
        return webdavUsername;
    }

    public void setWebdavUsername(String webdavUsername) {
        RuoYiConfig.webdavUsername = webdavUsername;
    }

    public static String getWebdavPassword() {
        return webdavPassword;
    }

    public void setWebdavPassword(String webdavPassword) {
        RuoYiConfig.webdavPassword = webdavPassword;
    }

    public static Integer getWebdavConnectTimeoutMs() {
        return webdavConnectTimeoutMs;
    }

    public void setWebdavConnectTimeoutMs(Integer webdavConnectTimeoutMs) {
        RuoYiConfig.webdavConnectTimeoutMs = webdavConnectTimeoutMs;
    }

    public static Integer getWebdavConnectionRequestTimeoutMs() {
        return webdavConnectionRequestTimeoutMs;
    }

    public void setWebdavConnectionRequestTimeoutMs(Integer webdavConnectionRequestTimeoutMs) {
        RuoYiConfig.webdavConnectionRequestTimeoutMs = webdavConnectionRequestTimeoutMs;
    }

    public static Integer getWebdavResponseTimeoutMs() {
        return webdavResponseTimeoutMs;
    }

    public void setWebdavResponseTimeoutMs(Integer webdavResponseTimeoutMs) {
        RuoYiConfig.webdavResponseTimeoutMs = webdavResponseTimeoutMs;
    }

    public static Integer getWebdavMaxConnTotal() {
        return webdavMaxConnTotal;
    }

    public void setWebdavMaxConnTotal(Integer webdavMaxConnTotal) {
        RuoYiConfig.webdavMaxConnTotal = webdavMaxConnTotal;
    }

    public static Integer getWebdavMaxConnPerRoute() {
        return webdavMaxConnPerRoute;
    }

    public void setWebdavMaxConnPerRoute(Integer webdavMaxConnPerRoute) {
        RuoYiConfig.webdavMaxConnPerRoute = webdavMaxConnPerRoute;
    }

}
