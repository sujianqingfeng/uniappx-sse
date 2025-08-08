#!/bin/bash

# Android AAR 构建脚本
# 支持不同环境：debug, release, release-minified

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 显示帮助信息
show_help() {
    echo "Android AAR 构建脚本"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -e, --env ENV        构建环境 (debug|release|release-minified) [默认: debug]"
    echo "  -c, --clean          清理构建缓存"
    echo "  -v, --verbose        详细输出"
    echo "  -h, --help           显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0                    # 构建 debug 版本"
    echo "  $0 -e release         # 构建 release 版本"
    echo "  $0 -e release-minified -c  # 构建混淆的 release 版本并清理缓存"
    echo ""
}

# 默认参数
BUILD_ENV="debug"
CLEAN_BUILD=false
VERBOSE=false

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--env)
            BUILD_ENV="$2"
            shift 2
            ;;
        -c|--clean)
            CLEAN_BUILD=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            print_error "未知参数: $1"
            show_help
            exit 1
            ;;
    esac
done

# 验证构建环境
case $BUILD_ENV in
    debug|release|release-minified)
        ;;
    *)
        print_error "无效的构建环境: $BUILD_ENV"
        print_error "支持的环境: debug, release, release-minified"
        exit 1
        ;;
esac

# 设置 Gradle 参数
GRADLE_ARGS=""
if [ "$VERBOSE" = true ]; then
    GRADLE_ARGS="$GRADLE_ARGS --info"
fi

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AAR_OUTPUT_DIR="$PROJECT_ROOT/sse-lib/build/outputs/aar"

print_info "开始构建 Android AAR 包..."
print_info "构建环境: $BUILD_ENV"
print_info "项目目录: $PROJECT_ROOT"

# 切换到项目目录
cd "$PROJECT_ROOT"

# 清理构建缓存（如果需要）
if [ "$CLEAN_BUILD" = true ]; then
    print_info "清理构建缓存..."
    ./gradlew clean $GRADLE_ARGS
fi

# 根据环境构建 AAR
case $BUILD_ENV in
    debug)
        print_info "构建 debug 版本..."
        ./gradlew :sse-lib:assembleDebug $GRADLE_ARGS
        AAR_FILE="$AAR_OUTPUT_DIR/sse-lib-debug.aar"
        ;;
    release)
        print_info "构建 release 版本..."
        ./gradlew :sse-lib:assembleRelease $GRADLE_ARGS
        AAR_FILE="$AAR_OUTPUT_DIR/sse-lib-release.aar"
        ;;
    release-minified)
        print_info "构建混淆的 release 版本..."
        # 临时修改 build.gradle.kts 启用混淆
        sed -i.bak 's/isMinifyEnabled = false/isMinifyEnabled = true/' sse-lib/build.gradle.kts
        ./gradlew :sse-lib:assembleRelease $GRADLE_ARGS
        # 恢复原始配置
        mv sse-lib/build.gradle.kts.bak sse-lib/build.gradle.kts
        AAR_FILE="$AAR_OUTPUT_DIR/sse-lib-release.aar"
        ;;
esac

# 检查构建结果
if [ -f "$AAR_FILE" ]; then
    AAR_SIZE=$(du -h "$AAR_FILE" | cut -f1)
    print_success "AAR 构建成功!"
    print_info "文件位置: $AAR_FILE"
    print_info "文件大小: $AAR_SIZE"
    
    # 显示文件信息
    print_info "AAR 文件详细信息:"
    ls -la "$AAR_FILE"
    
    # 复制到 UniApp 项目（如果存在）
    UNIAPP_AAR_DIR="$PROJECT_ROOT/../sse-uniapp-demo/uni_modules/sse-plugin/utssdk/app-android/libs"
    if [ -d "$UNIAPP_AAR_DIR" ]; then
        print_info "复制 AAR 到 UniApp 项目..."
        cp "$AAR_FILE" "$UNIAPP_AAR_DIR/"
        print_success "AAR 已复制到: $UNIAPP_AAR_DIR/"
    else
        print_warning "UniApp 项目目录不存在，跳过复制"
    fi
else
    print_error "AAR 构建失败!"
    exit 1
fi

print_success "构建完成!"
