#!/bin/bash

# dusk-module-auth 单元测试运行脚本
# 用于执行单元测试并生成覆盖率报告

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "=========================================="
echo "dusk-module-auth 单元测试运行脚本"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 1. 清理和编译
echo -e "${YELLOW}[1/4] 清理并编译项目...${NC}"
mvn clean compile test-compile
echo -e "${GREEN}✓ 编译完成${NC}"
echo ""

# 2. 运行所有单元测试
echo -e "${YELLOW}[2/4] 运行单元测试...${NC}"
mvn test
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ 所有测试通过${NC}"
else
    echo -e "${RED}✗ 部分测试失败${NC}"
    exit 1
fi
echo ""

# 3. 生成覆盖率报告
echo -e "${YELLOW}[3/4] 生成JaCoCo覆盖率报告...${NC}"
mvn jacoco:report
echo -e "${GREEN}✓ 覆盖率报告生成完成${NC}"
echo ""

# 4. 显示报告位置
echo -e "${YELLOW}[4/4] 显示报告信息...${NC}"
COVERAGE_REPORT="target/site/jacoco/index.html"
if [ -f "$COVERAGE_REPORT" ]; then
    echo -e "${GREEN}✓ 覆盖率报告已生成${NC}"
    echo ""
    echo "📊 覆盖率报告位置:"
    echo "   file://$PROJECT_DIR/$COVERAGE_REPORT"
    echo ""

    # 尝试打开报告
    if command -v open &> /dev/null; then
        echo -e "${YELLOW}正在打开覆盖率报告...${NC}"
        open "$COVERAGE_REPORT"
    elif command -v xdg-open &> /dev/null; then
        xdg-open "$COVERAGE_REPORT"
    fi
else
    echo -e "${RED}✗ 覆盖率报告未生成${NC}"
fi

echo ""
echo "=========================================="
echo -e "${GREEN}✓ 单元测试执行完成！${NC}"
echo "=========================================="

