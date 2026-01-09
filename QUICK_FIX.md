# 快速修复：Spring Boot 依赖冲突

## 问题症状

- ❌ `NoClassDefFoundError: com/google/protobuf/GeneratedMessageV3`
- ❌ `NoSuchMethodError: okio.BufferedSource.getBuffer()`

## 一键解决方案

直接复制以下配置到您的 `pom.xml` 的 `<dependencies>` 部分**最前面**：

```xml
<!-- 修复 VectorDB SDK 依赖冲突 -->
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

<!-- VectorDB SDK -->
<dependency>
    <groupId>com.tencent.tcvectordb</groupId>
    <artifactId>vectordatabase-sdk-java</artifactId>
    <version>2.6.0</version>
</dependency>
```

## Gradle 用户

```gradle
dependencies {
    implementation 'com.squareup.okio:okio:3.9.0'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.google.protobuf:protobuf-java:4.28.3'
    implementation 'com.google.protobuf:protobuf-java-util:4.28.3'
    implementation 'com.tencent.tcvectordb:vectordatabase-sdk-java:2.6.0'
}
```

## 执行命令

```bash
# Maven
mvn clean install

# Gradle
./gradlew clean build
```

## 仍然有问题？

查看详细文档：
- [Spring Boot 集成指南](./SPRING_BOOT_INTEGRATION.md) - 完整解决方案和故障排查
- [依赖问题技术分析](./DEPENDENCY_ANALYSIS.md) - 深入技术解释
