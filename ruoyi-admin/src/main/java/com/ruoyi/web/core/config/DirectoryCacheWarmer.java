package com.ruoyi.web.core.config;

import com.ruoyi.common.storage.DirectoryCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 应用启动后预热常用目录到 Redis 缓存，加速首页文件列表加载
 */
@Component
public class DirectoryCacheWarmer implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(DirectoryCacheWarmer.class);

    @Autowired
    private DirectoryCacheService directoryCacheService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("开始预热目录缓存...");
        try {
            directoryCacheService.warmupRootAndChildren();
        } catch (Exception e) {
            log.warn("目录缓存预热异常: {}", e.getMessage());
        }
    }
}
