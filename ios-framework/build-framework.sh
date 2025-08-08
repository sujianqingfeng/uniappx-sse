#!/bin/bash

# iOS Framework 构建脚本
# 支持构建 iOS framework 并自动复制到 UTS 插件目录

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目路径
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_NAME="ios-framework"
FRAMEWORK_NAME="ios_framework"
UTS_FRAMEWORK_DIR="../uniapp-x-playground/uni_modules/say-hi/utssdk/app-ios/Frameworks"

# 默认配置
BUILD_CONFIG="Release"
SIMULATOR_ARCHS="x86_64 arm64"
DEVICE_ARCHS="arm64"
CLEAN_BUILD=false
SHOW_HELP=false

# 显示帮助信息
show_help() {
    echo -e "${BLUE}iOS Framework 构建脚本${NC}"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -c, --config CONFIG    构建配置 (Debug|Release, 默认: Release)"
    echo "  -s, --simulator        仅构建模拟器版本"
    echo "  -d, --device           仅构建设备版本"
    echo "  -u, --universal        构建通用版本 (模拟器+设备)"
    echo "  --clean                清理构建缓存"
    echo "  -h, --help             显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0                      # 构建 Release 通用版本"
    echo "  $0 -c Debug             # 构建 Debug 通用版本"
    echo "  $0 -s                   # 仅构建模拟器版本"
    echo "  $0 -d                   # 仅构建设备版本"
    echo "  $0 --clean              # 清理后构建"
    echo ""
}

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -c|--config)
            BUILD_CONFIG="$2"
            shift 2
            ;;
        -s|--simulator)
            BUILD_TYPE="simulator"
            shift
            ;;
        -d|--device)
            BUILD_TYPE="device"
            shift
            ;;
        -u|--universal)
            BUILD_TYPE="universal"
            shift
            ;;
        --clean)
            CLEAN_BUILD=true
            shift
            ;;
        -h|--help)
            SHOW_HELP=true
            shift
            ;;
        *)
            echo -e "${RED}错误: 未知参数 $1${NC}"
            show_help
            exit 1
            ;;
    esac
done

# 显示帮助信息
if [ "$SHOW_HELP" = true ]; then
    show_help
    exit 0
fi

# 验证构建配置
if [[ "$BUILD_CONFIG" != "Debug" && "$BUILD_CONFIG" != "Release" ]]; then
    echo -e "${RED}错误: 无效的构建配置 '$BUILD_CONFIG'，必须是 Debug 或 Release${NC}"
    exit 1
fi

# 设置默认构建类型
if [ -z "$BUILD_TYPE" ]; then
    BUILD_TYPE="universal"
fi

echo -e "${BLUE}开始构建 iOS Framework...${NC}"
echo -e "项目目录: ${YELLOW}$PROJECT_DIR${NC}"
echo -e "构建配置: ${YELLOW}$BUILD_CONFIG${NC}"
echo -e "构建类型: ${YELLOW}$BUILD_TYPE${NC}"

# 检查 Xcode 是否可用
if ! command -v xcodebuild &> /dev/null; then
    echo -e "${RED}错误: 未找到 xcodebuild 命令，请确保已安装 Xcode${NC}"
    exit 1
fi

# 进入项目目录
cd "$PROJECT_DIR"

# 清理构建缓存
if [ "$CLEAN_BUILD" = true ]; then
    echo -e "${YELLOW}清理构建缓存...${NC}"
    xcodebuild clean -project "$PROJECT_NAME.xcodeproj" -scheme "$PROJECT_NAME" -configuration "$BUILD_CONFIG"
    rm -rf build/
fi

# 创建输出目录
OUTPUT_DIR="build/$BUILD_CONFIG"
mkdir -p "$OUTPUT_DIR"

# 构建函数
build_framework() {
    local platform=$1
    local archs=$2
    local sdk=$3
    
    echo -e "${BLUE}构建 $platform 版本 ($archs)...${NC}"
    
    # 如果指定了多个架构，需要分别构建然后合并
    if [[ "$archs" == *" "* ]]; then
        local temp_frameworks=()
        local arch_array=($archs)
        
        for arch in "${arch_array[@]}"; do
            echo -e "${YELLOW}构建架构: $arch${NC}"
            
            xcodebuild build \
                -project "$PROJECT_NAME.xcodeproj" \
                -scheme "$PROJECT_NAME" \
                -configuration "$BUILD_CONFIG" \
                -sdk "$sdk" \
                -arch "$arch" \
                -derivedDataPath "build/DerivedData" \
                ONLY_ACTIVE_ARCH=NO \
                SKIP_INSTALL=NO \
                BUILD_LIBRARY_FOR_DISTRIBUTION=YES \
                SUPPORTS_MACCATALYST=NO \
                SUPPORTS_MAC_DESIGNED_FOR_IPHONE_IPAD=NO
            
            # 查找构建的 framework
            local framework_path="build/DerivedData/Build/Products/$BUILD_CONFIG-$sdk/$FRAMEWORK_NAME.framework"
            local temp_path="$OUTPUT_DIR/$FRAMEWORK_NAME-$platform-$arch.framework"
            
            if [ -d "$framework_path" ]; then
                cp -R "$framework_path" "$temp_path"
                temp_frameworks+=("$temp_path")
                echo -e "${GREEN}✓ 架构 $arch 构建完成${NC}"
            else
                echo -e "${RED}✗ 架构 $arch 构建失败${NC}"
                exit 1
            fi
        done
        
        # 合并多个架构的 framework
        local output_path="$OUTPUT_DIR/$FRAMEWORK_NAME-$platform.framework"
        cp -R "${temp_frameworks[0]}" "$output_path"
        
        # 合并二进制文件
        local binary_name="$FRAMEWORK_NAME"
        local universal_binary="$output_path/$binary_name"
        local lipo_args=()
        
        for temp_framework in "${temp_frameworks[@]}"; do
            lipo_args+=("$temp_framework/$binary_name")
        done
        
        lipo -create "${lipo_args[@]}" -output "$universal_binary"
        
        # 清理临时文件
        for temp_framework in "${temp_frameworks[@]}"; do
            rm -rf "$temp_framework"
        done
        
        echo -e "${GREEN}✓ $platform 版本构建完成: $output_path${NC}"
    else
        # 单个架构构建
        xcodebuild build \
            -project "$PROJECT_NAME.xcodeproj" \
            -scheme "$PROJECT_NAME" \
            -configuration "$BUILD_CONFIG" \
            -sdk "$sdk" \
            -arch "$archs" \
            -derivedDataPath "build/DerivedData" \
            ONLY_ACTIVE_ARCH=NO \
            SKIP_INSTALL=NO \
            BUILD_LIBRARY_FOR_DISTRIBUTION=YES \
            SUPPORTS_MACCATALYST=NO \
            SUPPORTS_MAC_DESIGNED_FOR_IPHONE_IPAD=NO
        
        # 查找并复制 framework 到输出目录
        local framework_path="build/DerivedData/Build/Products/$BUILD_CONFIG-$sdk/$FRAMEWORK_NAME.framework"
        local output_path="$OUTPUT_DIR/$FRAMEWORK_NAME-$platform.framework"
        
        if [ -d "$framework_path" ]; then
            cp -R "$framework_path" "$output_path"
            echo -e "${GREEN}✓ $platform 版本构建完成: $output_path${NC}"
        else
            echo -e "${RED}✗ $platform 版本构建失败${NC}"
            exit 1
        fi
    fi
}

# 创建通用 framework
create_universal_framework() {
    echo -e "${BLUE}创建通用 Framework...${NC}"
    
    local simulator_framework="$OUTPUT_DIR/$FRAMEWORK_NAME-simulator.framework"
    local device_framework="$OUTPUT_DIR/$FRAMEWORK_NAME-device.framework"
    local universal_framework="$OUTPUT_DIR/$FRAMEWORK_NAME.framework"
    
    if [ ! -d "$simulator_framework" ] || [ ! -d "$device_framework" ]; then
        echo -e "${RED}错误: 缺少模拟器或设备版本的 framework${NC}"
        exit 1
    fi
    
    # 检查架构是否相同
    local simulator_archs=$(lipo -info "$simulator_framework/$FRAMEWORK_NAME" | sed 's/.*are: //' | tr -d ' ')
    local device_archs=$(lipo -info "$device_framework/$FRAMEWORK_NAME" | sed 's/.*is architecture: //')
    
    # 如果模拟器版本只包含设备版本的架构，则直接使用设备版本
    if [[ "$simulator_archs" == "$device_archs" ]]; then
        echo -e "${YELLOW}模拟器和设备版本架构相同 ($device_archs)，直接使用设备版本${NC}"
        cp -R "$device_framework" "$universal_framework"
        echo -e "${GREEN}✓ 通用 Framework 创建完成: $universal_framework${NC}"
    else
        # 复制设备版本作为基础
        cp -R "$device_framework" "$universal_framework"
        
        # 合并二进制文件
        local binary_name="$FRAMEWORK_NAME"
        local simulator_binary="$simulator_framework/$binary_name"
        local device_binary="$device_framework/$binary_name"
        local universal_binary="$universal_framework/$binary_name"
        
        if [ -f "$simulator_binary" ] && [ -f "$device_binary" ]; then
            # 检查是否有架构冲突
            local simulator_arm64=$(lipo -info "$simulator_binary" | grep -q "arm64" && echo "yes" || echo "no")
            local device_arm64=$(lipo -info "$device_binary" | grep -q "arm64" && echo "yes" || echo "no")
            
            if [[ "$simulator_arm64" == "yes" && "$device_arm64" == "yes" ]]; then
                # 如果两个版本都包含 arm64，需要特殊处理
                echo -e "${YELLOW}检测到架构冲突，使用模拟器版本（包含更多架构）${NC}"
                cp "$simulator_binary" "$universal_binary"
            else
                # 正常合并
                lipo -create "$simulator_binary" "$device_binary" -output "$universal_binary"
            fi
            echo -e "${GREEN}✓ 通用 Framework 创建完成: $universal_framework${NC}"
        else
            echo -e "${RED}✗ 创建通用 Framework 失败${NC}"
            exit 1
        fi
    fi
}

# 根据构建类型执行构建
case "$BUILD_TYPE" in
    "simulator")
        build_framework "simulator" "$SIMULATOR_ARCHS" "iphonesimulator"
        FINAL_FRAMEWORK="$OUTPUT_DIR/$FRAMEWORK_NAME-simulator.framework"
        ;;
    "device")
        build_framework "device" "$DEVICE_ARCHS" "iphoneos"
        FINAL_FRAMEWORK="$OUTPUT_DIR/$FRAMEWORK_NAME-device.framework"
        ;;
    "universal")
        build_framework "simulator" "$SIMULATOR_ARCHS" "iphonesimulator"
        build_framework "device" "$DEVICE_ARCHS" "iphoneos"
        create_universal_framework
        FINAL_FRAMEWORK="$OUTPUT_DIR/$FRAMEWORK_NAME.framework"
        ;;
esac

# 复制到 UTS 插件目录
if [ -d "$UTS_FRAMEWORK_DIR" ]; then
    echo -e "${BLUE}复制 Framework 到 UTS 插件目录...${NC}"
    
    # 备份原有 framework
    if [ -d "$UTS_FRAMEWORK_DIR/$FRAMEWORK_NAME.framework" ]; then
        mv "$UTS_FRAMEWORK_DIR/$FRAMEWORK_NAME.framework" "$UTS_FRAMEWORK_DIR/$FRAMEWORK_NAME.framework.backup"
        echo -e "${YELLOW}已备份原有 framework${NC}"
    fi
    
    # 复制新的 framework
    cp -R "$FINAL_FRAMEWORK" "$UTS_FRAMEWORK_DIR/"
    echo -e "${GREEN}✓ Framework 已复制到: $UTS_FRAMEWORK_DIR/$FRAMEWORK_NAME.framework${NC}"
    
    # 清理备份
    if [ -d "$UTS_FRAMEWORK_DIR/$FRAMEWORK_NAME.framework.backup" ]; then
        rm -rf "$UTS_FRAMEWORK_DIR/$FRAMEWORK_NAME.framework.backup"
    fi
else
    echo -e "${YELLOW}警告: UTS Framework 目录不存在: $UTS_FRAMEWORK_DIR${NC}"
    echo -e "${YELLOW}请手动复制 $FINAL_FRAMEWORK 到 UTS 插件目录${NC}"
fi

echo -e "${GREEN}🎉 iOS Framework 构建完成!${NC}"
echo -e "输出位置: ${YELLOW}$FINAL_FRAMEWORK${NC}"
