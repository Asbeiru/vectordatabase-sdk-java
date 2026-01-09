#!/bin/bash

# Vector Database SDK Java - 快速验证脚本
# 此脚本帮助你快速验证项目是否可以正常使用

echo "======================================"
echo "Vector Database SDK Java - 快速验证"
echo "======================================"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 步骤 1: 检查 Java 环境
echo -e "${YELLOW}[1/5] 检查 Java 环境...${NC}"
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    echo -e "${GREEN}✓ Java 已安装: $JAVA_VERSION${NC}"
else
    echo -e "${RED}✗ 未找到 Java，请先安装 JDK 8 或更高版本${NC}"
    exit 1
fi
echo ""

# 步骤 2: 检查 Maven 环境
echo -e "${YELLOW}[2/5] 检查 Maven 环境...${NC}"
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -n 1)
    echo -e "${GREEN}✓ Maven 已安装: $MVN_VERSION${NC}"
else
    echo -e "${RED}✗ 未找到 Maven，请先安装 Apache Maven${NC}"
    exit 1
fi
echo ""

# 步骤 3: 检查项目结构
echo -e "${YELLOW}[3/5] 检查项目结构...${NC}"
if [ -d "tcvectordb" ]; then
    echo -e "${GREEN}✓ tcvectordb 目录存在${NC}"
else
    echo -e "${RED}✗ 未找到 tcvectordb 目录${NC}"
    exit 1
fi

if [ -f "tcvectordb/pom.xml" ]; then
    echo -e "${GREEN}✓ pom.xml 文件存在${NC}"
else
    echo -e "${RED}✗ 未找到 pom.xml 文件${NC}"
    exit 1
fi
echo ""

# 步骤 4: 编译项目
echo -e "${YELLOW}[4/5] 编译项目（这可能需要几分钟）...${NC}"
cd tcvectordb

if mvn clean compile -DskipTests > /tmp/mvn-build.log 2>&1; then
    echo -e "${GREEN}✓ 编译成功${NC}"

    # 检查生成的 class 文件
    if [ -d "target/classes" ]; then
        CLASS_COUNT=$(find target/classes -name "*.class" | wc -l)
        echo -e "${GREEN}  - 生成了 $CLASS_COUNT 个 class 文件${NC}"
    fi
else
    echo -e "${RED}✗ 编译失败${NC}"
    echo -e "${YELLOW}查看详细日志: cat /tmp/mvn-build.log${NC}"
    cd ..
    exit 1
fi

cd ..
echo ""

# 步骤 5: 检查示例文件
echo -e "${YELLOW}[5/5] 检查示例文件...${NC}"
EXAMPLE_DIR="tcvectordb/src/main/java/com/tencent/tcvectordb/examples"
if [ -d "$EXAMPLE_DIR" ]; then
    EXAMPLE_COUNT=$(find $EXAMPLE_DIR -name "*.java" | grep -v "CommonService" | wc -l)
    echo -e "${GREEN}✓ 找到 $EXAMPLE_COUNT 个示例文件:${NC}"

    # 列出所有示例
    find $EXAMPLE_DIR -name "*Example.java" -exec basename {} \; | while read example; do
        echo "  - $example"
    done
else
    echo -e "${RED}✗ 未找到示例目录${NC}"
fi
echo ""

# 总结
echo "======================================"
echo -e "${GREEN}✓ 验证完成！${NC}"
echo "======================================"
echo ""
echo "📖 后续步骤："
echo ""
echo "1. 配置数据库连接参数："
echo "   编辑 tcvectordb/src/main/java/com/tencent/tcvectordb/examples/CommonService.java"
echo "   修改 vdbURL 和 vdbKey 为你的腾讯云向量数据库凭证"
echo ""
echo "2. 运行示例代码："
echo "   cd tcvectordb"
echo "   mvn exec:java -Dexec.mainClass=\"com.tencent.tcvectordb.examples.VectorDBExample\""
echo ""
echo "3. 查看完整使用指南："
echo "   cat USAGE_GUIDE.md"
echo ""
echo "📚 更多资源："
echo "   - 官方文档: https://cloud.tencent.com/document/product/1709"
echo "   - API 文档: https://cloud.tencent.com/document/product/1709/97768"
echo "   - GitHub: https://github.com/Tencent/vectordatabase-sdk-java"
echo ""
