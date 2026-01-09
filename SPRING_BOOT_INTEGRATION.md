# Spring Boot 集成指南

> **💡 想了解为什么会出现依赖问题？** 请参阅 [依赖问题技术分析](./DEPENDENCY_ANALYSIS.md)，详细解释了这是 SDK 的 Bug 还是项目配置问题。

## 快速开始：推荐的完整依赖配置

为了避免各种依赖冲突，推荐在 `pom.xml` 中按以下顺序声明依赖：

```xml
<dependencies>
    <!-- 1. 显式声明基础库版本，避免冲突 -->
    <dependency>
        <groupId>com.squareup.okio</groupId>
        <artifactId>okio</artifactId>
        <version>3.9.0</version>
    </dependency>
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.12.0</version>
    </dependency>
    <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java</artifactId>
        <version>4.28.3</version>
    </dependency>
    <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java-util</artifactId>
        <version>4.28.3</version>
    </dependency>

    <!-- 2. VectorDB SDK -->
    <dependency>
        <groupId>com.tencent.tcvectordb</groupId>
        <artifactId>vectordatabase-sdk-java</artifactId>
        <version>2.6.0</version>
    </dependency>

    <!-- 3. 您的其他依赖 -->
    <!-- ... -->
</dependencies>
```

**Gradle 用户**：

```gradle
dependencies {
    implementation 'com.squareup.okio:okio:3.9.0'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.google.protobuf:protobuf-java:4.28.3'
    implementation 'com.google.protobuf:protobuf-java-util:4.28.3'
    implementation 'com.tencent.tcvectordb:vectordatabase-sdk-java:2.6.0'
}
```

> **⚠️ 重要**：这些基础库依赖必须放在 VectorDB SDK 之前声明，以确保使用正确的版本。

---

## 常见问题

### 问题 1：NoClassDefFoundError: com/google/protobuf/GeneratedMessageV3

#### 问题描述

在 Spring Boot 项目中使用 VectorDB SDK 时，可能会遇到以下错误：

```
java.lang.NoClassDefFoundError: com/google/protobuf/GeneratedMessageV3
Caused by: java.lang.ClassNotFoundException: com.google.protobuf.GeneratedMessageV3
```

#### 根本原因

此错误由以下原因之一引起：

1. **依赖冲突**：项目中存在多个版本的 protobuf 依赖，Maven/Gradle 选择了较旧的版本
2. **依赖传递问题**：某些依赖排除了 protobuf，导致运行时缺少必要的类
3. **Spring Boot 版本冲突**：某些 Spring Boot 版本内置的依赖管理可能覆盖了 SDK 所需的 protobuf 版本

### 解决方案

#### 方案 1：显式声明 protobuf 依赖（推荐）

在您的 `pom.xml` 中，在 VectorDB SDK 依赖**之前**显式声明 protobuf 依赖：

```xml
<dependencies>
    <!-- 显式声明 protobuf 版本 -->
    <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java</artifactId>
        <version>4.28.3</version>
    </dependency>
    <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java-util</artifactId>
        <version>4.28.3</version>
    </dependency>

    <!-- VectorDB SDK -->
    <dependency>
        <groupId>com.tencent.tcvectordb</groupId>
        <artifactId>vectordatabase-sdk-java</artifactId>
        <version>2.6.0</version>
    </dependency>
</dependencies>
```

如果使用 Gradle：

```gradle
dependencies {
    // 显式声明 protobuf 版本
    implementation 'com.google.protobuf:protobuf-java:4.28.3'
    implementation 'com.google.protobuf:protobuf-java-util:4.28.3'

    // VectorDB SDK
    implementation 'com.tencent.tcvectordb:vectordatabase-sdk-java:2.6.0'
}
```

#### 方案 2：使用 dependencyManagement 强制版本

在 Maven 项目中，使用 `dependencyManagement` 强制指定版本：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.google.protobuf</groupId>
            <artifactId>protobuf-java</artifactId>
            <version>4.28.3</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### 方案 3：检查并解决依赖冲突

使用 Maven 命令检查依赖树：

```bash
mvn dependency:tree -Dincludes=com.google.protobuf:protobuf-java
```

或者使用 Gradle：

```bash
./gradlew dependencies --configuration runtimeClasspath | grep protobuf
```

找到冲突的依赖后，在对应的依赖中排除旧版本的 protobuf：

```xml
<dependency>
    <groupId>some.other</groupId>
    <artifactId>dependency</artifactId>
    <version>x.y.z</version>
    <exclusions>
        <exclusion>
            <groupId>com.google.protobuf</groupId>
            <artifactId>protobuf-java</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

#### 方案 4：Spring Boot 项目特殊处理

如果您使用 Spring Boot，可能需要覆盖 Spring Boot 的依赖管理。在 `pom.xml` 的 `<properties>` 中添加：

```xml
<properties>
    <protobuf.version>4.28.3</protobuf.version>
    <grpc.version>1.61.1</grpc.version>
</properties>
```

---

### 问题 2：NoSuchMethodError: okio.BufferedSource.getBuffer()

#### 问题描述

解决 protobuf 依赖问题后，可能会遇到新的错误：

```
io.grpc.StatusRuntimeException: INTERNAL: error in frame handler
Caused by: java.lang.NoSuchMethodError: okio.BufferedSource.getBuffer()Lokio/Buffer;
    at io.grpc.okhttp.OkHttpClientTransport$ClientFrameHandler.data(OkHttpClientTransport.java:1158)
```

#### 根本原因

这是 **okio 库的版本冲突**：

1. SDK 使用的 `okhttp 4.9.2` 和 `grpc-okhttp 1.61.1` 需要 **okio 2.8.0+** 版本
2. 您的项目中可能存在其他依赖引入了**旧版本 okio（1.x）**
3. `BufferedSource.getBuffer()` 方法在不同版本的 okio 中签名不同

常见冲突来源：
- 旧版本的 okhttp3（3.x）依赖 okio 1.x
- 某些 Android 或其他库依赖旧版本 okio

#### 解决方案：显式声明 okio 和 okhttp 版本

在您的 `pom.xml` 中添加（在所有其他依赖之前）：

```xml
<dependencies>
    <!-- 显式声明 okio 版本 -->
    <dependency>
        <groupId>com.squareup.okio</groupId>
        <artifactId>okio</artifactId>
        <version>3.9.0</version>
    </dependency>

    <!-- 显式声明 okhttp 版本 -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.12.0</version>
    </dependency>

    <!-- 显式声明 protobuf 版本 -->
    <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java</artifactId>
        <version>4.28.3</version>
    </dependency>
    <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java-util</artifactId>
        <version>4.28.3</version>
    </dependency>

    <!-- VectorDB SDK -->
    <dependency>
        <groupId>com.tencent.tcvectordb</groupId>
        <artifactId>vectordatabase-sdk-java</artifactId>
        <version>2.6.0</version>
    </dependency>
</dependencies>
```

如果使用 Gradle：

```gradle
dependencies {
    // 显式声明版本
    implementation 'com.squareup.okio:okio:3.9.0'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.google.protobuf:protobuf-java:4.28.3'
    implementation 'com.google.protobuf:protobuf-java-util:4.28.3'

    // VectorDB SDK
    implementation 'com.tencent.tcvectordb:vectordatabase-sdk-java:2.6.0'
}
```

#### 排查 okio 冲突

使用 Maven 命令检查 okio 依赖树：

```bash
mvn dependency:tree -Dincludes=com.squareup.okio:okio
```

或者使用 Gradle：

```bash
./gradlew dependencies --configuration runtimeClasspath | grep okio
```

查找输出中的冲突信息，例如：

```
[INFO] |  +- com.squareup.okio:okio:jar:3.9.0:compile
[INFO] |  +- (com.squareup.okio:okio:jar:3.9.0:compile - omitted for conflict with 1.17.2)
```

如果发现其他依赖引入了旧版本 okio，可以排除它：

```xml
<dependency>
    <groupId>some.other</groupId>
    <artifactId>dependency</artifactId>
    <version>x.y.z</version>
    <exclusions>
        <exclusion>
            <groupId>com.squareup.okio</groupId>
            <artifactId>okio</artifactId>
        </exclusion>
        <exclusion>
            <groupId>com.squareup.okhttp3</groupId>
            <artifactId>okhttp</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

---

### Spring Boot 配置示例

#### 配置类示例

```java
package com.example.config;

import com.tencent.tcvectordb.client.VectorDBClient;
import com.tencent.tcvectordb.model.ConnectParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorDBConfig {

    @Value("${vectordb.url}")
    private String url;

    @Value("${vectordb.username:root}")
    private String username;

    @Value("${vectordb.key}")
    private String key;

    @Value("${vectordb.timeout:30}")
    private Integer timeout;

    @Bean
    public VectorDBClient vectorDBClient() {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withUrl(url)
                .withUsername(username)
                .withKey(key)
                .withTimeout(timeout)
                .build();

        return new VectorDBClient(connectParam);
    }
}
```

#### application.yml 配置

```yaml
vectordb:
  url: http://your-vectordb-instance:port
  username: root
  key: your-api-key
  timeout: 30
```

### 验证解决方案

重新构建并运行项目：

```bash
# Maven
mvn clean install
mvn spring-boot:run

# Gradle
./gradlew clean build
./gradlew bootRun
```

### 如果问题仍然存在

1. 清理本地 Maven/Gradle 缓存：
   ```bash
   # Maven
   mvn dependency:purge-local-repository

   # Gradle
   ./gradlew cleanBuildCache
   rm -rf ~/.gradle/caches/
   ```

2. 检查 IDE 的依赖缓存（如 IntelliJ IDEA）：
   - File → Invalidate Caches / Restart

3. 确保使用正确的 Java 版本（Java 8 或更高版本）

4. 检查是否有多个类加载器导致的问题（特别是在使用热部署工具如 Spring DevTools 时）

### 需要帮助？

如果您遇到其他问题，请在 [GitHub Issues](https://github.com/Tencent/vectordatabase-sdk-java/issues) 中提交问题，并提供：

- 完整的错误堆栈信息
- `pom.xml` 或 `build.gradle` 文件
- Spring Boot 版本
- Java 版本
- `mvn dependency:tree` 的输出
