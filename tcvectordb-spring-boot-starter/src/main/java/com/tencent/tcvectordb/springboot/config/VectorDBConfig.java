package com.tencent.tcvectordb.springboot.config;

import com.tencent.tcvectordb.client.RPCVectorDBClient;
import com.tencent.tcvectordb.client.VectorDBClient;
import com.tencent.tcvectordb.model.param.database.ConnectParam;
import com.tencent.tcvectordb.model.param.enums.ReadConsistencyEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;

/**
 * 腾讯云向量数据库配置类
 * 管理 VectorDBClient Bean的生命周期
 *
 * 核心职责:
 * 1. 根据application.yml配置创建VectorDBClient实例
 * 2. 在应用启动时建立数据库连接
 * 3. 在应用关闭时优雅关闭连接
 *
 * @author Tencent Cloud VectorDB Team
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(VectorDBProperties.class)
public class VectorDBConfig {

    @Autowired
    private VectorDBProperties properties;

    private VectorDBClient vectorDBClient;

    /**
     * 创建VectorDBClient Bean
     *
     * 使用官方SDK的RPCVectorDBClient实现,相比HTTP客户端具有更好的性能
     *
     * ConnectParam配置说明:
     * - url: 向量数据库的连接地址
     * - username: 用户名,默认为root
     * - key: API Key,用于身份认证
     * - timeout: 连接超时时间(秒)
     * - maxIdleConnections: 连接池中最大空闲连接数
     *
     * ReadConsistencyEnum说明:
     * - EVENTUAL_CONSISTENCY: 最终一致性,性能更好,适合大多数场景
     * - STRONG_CONSISTENCY: 强一致性,保证数据立即可见
     *
     * @return VectorDBClient实例
     */
    @Bean
    public VectorDBClient vectorDBClient() {
        log.info("初始化腾讯云向量数据库客户端...");
        log.info("连接URL: {}", properties.getUrl());
        log.info("用户名: {}", properties.getUsername());
        log.info("超时设置: {}秒", properties.getTimeout());

        try {
            // 构建连接参数
            ConnectParam connectParam = ConnectParam.newBuilder()
                    .withUrl(properties.getUrl())
                    .withUsername(properties.getUsername())
                    .withKey(properties.getApiKey())
                    .withTimeout(properties.getTimeout())
                    .withMaxIdleConnections(properties.getMaxIdleConnections())
                    .build();

            // 创建RPC客户端,使用最终一致性以获得更好的性能
            // 官方推荐使用RPCVectorDBClient,相比VectorDBClient(HTTP)性能更好
            this.vectorDBClient = new RPCVectorDBClient(
                    connectParam,
                    ReadConsistencyEnum.EVENTUAL_CONSISTENCY
            );

            log.info("向量数据库客户端初始化成功");

            // 执行健康检查
            performHealthCheck();

            return this.vectorDBClient;

        } catch (Exception e) {
            log.error("向量数据库客户端初始化失败", e);
            throw new RuntimeException("无法连接到腾讯云向量数据库: " + e.getMessage(), e);
        }
    }

    /**
     * 数据库健康检查
     * 在客户端初始化后,尝试列出数据库以验证连接是否正常
     */
    private void performHealthCheck() {
        try {
            log.info("执行数据库健康检查...");

            // 尝试列出数据库,验证连接是否正常
            // 使用官方SDK的listDatabase()方法
            vectorDBClient.listDatabase();

            log.info("数据库健康检查通过");
        } catch (Exception e) {
            log.warn("数据库健康检查失败,但不影响启动: {}", e.getMessage());
            // 不抛出异常,允许应用继续启动
            // 某些场景下可能是数据库还未创建,后续会自动创建
        }
    }

    /**
     * 应用关闭时清理资源
     * 优雅关闭VectorDBClient连接
     */
    @PreDestroy
    public void destroy() {
        if (vectorDBClient != null) {
            try {
                log.info("正在关闭向量数据库连接...");
                vectorDBClient.close();
                log.info("向量数据库连接已关闭");
            } catch (Exception e) {
                log.error("关闭向量数据库连接时出错", e);
            }
        }
    }
}
