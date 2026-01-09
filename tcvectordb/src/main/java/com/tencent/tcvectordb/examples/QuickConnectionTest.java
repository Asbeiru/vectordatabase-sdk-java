/*
 * Quick Connection Test - 快速连接测试
 *
 * 此文件用于快速验证你的数据库连接配置是否正确
 * 使用方法：
 * 1. 在 CommonService.java 中配置你的 vdbURL 和 vdbKey
 * 2. 运行此文件
 */

package com.tencent.tcvectordb.examples;

import com.tencent.tcvectordb.client.VectorDBClient;
import com.tencent.tcvectordb.model.Database;

import java.util.List;

public class QuickConnectionTest {

    public static void main(String[] args) {
        System.out.println("============================================");
        System.out.println("  Vector Database SDK - 快速连接测试");
        System.out.println("============================================");
        System.out.println();

        VectorDBClient client = null;

        try {
            // 步骤 1: 初始化客户端
            System.out.println("[1/3] 初始化数据库客户端...");
            client = CommonService.initClient();
            System.out.println("✓ 客户端初始化成功");
            System.out.println();

            // 步骤 2: 列出所有数据库
            System.out.println("[2/3] 获取数据库列表...");
            List<String> databases = client.listDatabases();
            System.out.println("✓ 成功连接到数据库服务器");
            System.out.println("  当前存在 " + databases.size() + " 个数据库:");

            if (databases.isEmpty()) {
                System.out.println("  （暂无数据库）");
            } else {
                for (String dbName : databases) {
                    System.out.println("  - " + dbName);
                }
            }
            System.out.println();

            // 步骤 3: 测试基本操作
            System.out.println("[3/3] 测试基本操作...");

            // 创建测试数据库名称
            String testDbName = "test_connection_" + System.currentTimeMillis();
            System.out.println("  尝试创建测试数据库: " + testDbName);

            try {
                Database testDb = client.createDatabase(testDbName);
                System.out.println("✓ 测试数据库创建成功");

                // 立即删除测试数据库
                client.dropDatabase(testDbName);
                System.out.println("✓ 测试数据库删除成功");
            } catch (Exception e) {
                System.out.println("✗ 测试操作失败: " + e.getMessage());
                System.out.println("  可能的原因：权限不足或数据库已存在");
            }

            System.out.println();
            System.out.println("============================================");
            System.out.println("✓ 连接测试完成！");
            System.out.println("============================================");
            System.out.println();
            System.out.println("下一步：");
            System.out.println("1. 运行其他示例程序进行功能测试");
            System.out.println("2. 开始构建你自己的向量检索应用");
            System.out.println();

        } catch (Exception e) {
            System.err.println();
            System.err.println("============================================");
            System.err.println("✗ 连接测试失败！");
            System.err.println("============================================");
            System.err.println();
            System.err.println("错误信息: " + e.getMessage());
            System.err.println();
            System.err.println("请检查：");
            System.err.println("1. CommonService.java 中的 vdbURL 是否正确");
            System.err.println("2. CommonService.java 中的 vdbKey 是否正确");
            System.err.println("3. 网络连接是否正常");
            System.err.println("4. 数据库实例是否已启动");
            System.err.println();
            System.err.println("详细错误堆栈：");
            e.printStackTrace();
            System.exit(1);
        } finally {
            // 关闭客户端
            if (client != null) {
                try {
                    client.close();
                    System.out.println("数据库连接已关闭");
                } catch (Exception e) {
                    System.err.println("关闭连接时出错: " + e.getMessage());
                }
            }
        }
    }
}
