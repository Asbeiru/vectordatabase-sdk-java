# Tencent VectorDB Java SDK

Java SDK for [Tencent Cloud VectorDB](https://cloud.tencent.com/product/vdb).

## Getting started

### Docs
 - [Create database instance](https://cloud.tencent.com/document/product/1709/94951)
 - [API Docs](https://cloud.tencent.com/document/product/1709/97768)


### Prerequisites

    -   Java 8 or higher
    -   Apache Maven or Gradle/Grails

### Install Java SDK

You can use **Apache Maven** or **Gradle**/**Grails** to download the SDK.

   - Apache Maven

       ```xml
        <dependency>
            <groupId>com.tencent.tcvectordb</groupId>
            <artifactId>vectordatabase-sdk-java</artifactId>
            <version>2.6.0</version>
        </dependency>
       ```

   - Gradle/Grails

        ```gradle
        compile 'com.tencent.tcvectordb:vectordatabase-sdk-java:2.6.0'
        ```

#### Spring Boot Integration

If you are using this SDK in a Spring Boot project, please refer to [Spring Boot Integration Guide](./SPRING_BOOT_INTEGRATION.md) for dependency configuration and common troubleshooting.

### Examples

Please refer to [examples](./tcvectordb/src/main/java/com/tencent/tcvectordb/examples) folder for Java SDK examples.