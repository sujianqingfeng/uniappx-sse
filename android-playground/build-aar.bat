@echo off
setlocal enabledelayedexpansion

REM Android AAR 构建脚本 (Windows 版本)
REM 支持不同环境：debug, release, release-minified

set "BUILD_ENV=debug"
set "CLEAN_BUILD=false"
set "VERBOSE=false"

REM 解析命令行参数
:parse_args
if "%~1"=="" goto :end_parse
if /i "%~1"=="-e" (
    set "BUILD_ENV=%~2"
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="--env" (
    set "BUILD_ENV=%~2"
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="-c" (
    set "CLEAN_BUILD=true"
    shift
    goto :parse_args
)
if /i "%~1"=="--clean" (
    set "CLEAN_BUILD=true"
    shift
    goto :parse_args
)
if /i "%~1"=="-v" (
    set "VERBOSE=true"
    shift
    goto :parse_args
)
if /i "%~1"=="--verbose" (
    set "VERBOSE=true"
    shift
    goto :parse_args
)
if /i "%~1"=="-h" (
    goto :show_help
)
if /i "%~1"=="--help" (
    goto :show_help
)
echo [ERROR] 未知参数: %~1
goto :show_help

:end_parse

REM 验证构建环境
if not "%BUILD_ENV%"=="debug" (
    if not "%BUILD_ENV%"=="release" (
        if not "%BUILD_ENV%"=="release-minified" (
            echo [ERROR] 无效的构建环境: %BUILD_ENV%
            echo [ERROR] 支持的环境: debug, release, release-minified
            exit /b 1
        )
    )
)

REM 设置 Gradle 参数
set "GRADLE_ARGS="
if "%VERBOSE%"=="true" (
    set "GRADLE_ARGS=--info"
)

REM 项目根目录
set "PROJECT_ROOT=%~dp0"
set "AAR_OUTPUT_DIR=%PROJECT_ROOT%android-lib\build\outputs\aar"

echo [INFO] 开始构建 Android AAR 包...
echo [INFO] 构建环境: %BUILD_ENV%
echo [INFO] 项目目录: %PROJECT_ROOT%

REM 切换到项目目录
cd /d "%PROJECT_ROOT%"

REM 清理构建缓存（如果需要）
if "%CLEAN_BUILD%"=="true" (
    echo [INFO] 清理构建缓存...
    gradlew.bat clean %GRADLE_ARGS%
    if errorlevel 1 (
        echo [ERROR] 清理失败!
        exit /b 1
    )
)

REM 根据环境构建 AAR
if "%BUILD_ENV%"=="debug" (
    echo [INFO] 构建 debug 版本...
    gradlew.bat :android-lib:assembleDebug %GRADLE_ARGS%
    set "AAR_FILE=%AAR_OUTPUT_DIR%\android-lib-debug.aar"
) else if "%BUILD_ENV%"=="release" (
    echo [INFO] 构建 release 版本...
    gradlew.bat :android-lib:assembleRelease %GRADLE_ARGS%
    set "AAR_FILE=%AAR_OUTPUT_DIR%\android-lib-release.aar"
) else if "%BUILD_ENV%"=="release-minified" (
    echo [INFO] 构建混淆的 release 版本...
    REM 临时修改 build.gradle.kts 启用混淆
    powershell -Command "(Get-Content android-lib\build.gradle.kts) -replace 'isMinifyEnabled = false', 'isMinifyEnabled = true' | Set-Content android-lib\build.gradle.kts"
    gradlew.bat :android-lib:assembleRelease %GRADLE_ARGS%
    REM 恢复原始配置
    powershell -Command "(Get-Content android-lib\build.gradle.kts) -replace 'isMinifyEnabled = true', 'isMinifyEnabled = false' | Set-Content android-lib\build.gradle.kts"
    set "AAR_FILE=%AAR_OUTPUT_DIR%\android-lib-release.aar"
)

if errorlevel 1 (
    echo [ERROR] AAR 构建失败!
    exit /b 1
)

REM 检查构建结果
if exist "%AAR_FILE%" (
    echo [SUCCESS] AAR 构建成功!
    echo [INFO] 文件位置: %AAR_FILE%
    
    REM 显示文件信息
    echo [INFO] AAR 文件详细信息:
    dir "%AAR_FILE%"
    
    REM 复制到 UniApp 项目（如果存在）
    set "UNIAPP_AAR_DIR=%PROJECT_ROOT%..\uniapp-x-playground\uni_modules\say-hi\utssdk\app-android\libs"
    if exist "%UNIAPP_AAR_DIR%" (
        echo [INFO] 复制 AAR 到 UniApp 项目...
        copy "%AAR_FILE%" "%UNIAPP_AAR_DIR%\"
        echo [SUCCESS] AAR 已复制到: %UNIAPP_AAR_DIR%\
    ) else (
        echo [WARNING] UniApp 项目目录不存在，跳过复制
    )
) else (
    echo [ERROR] AAR 构建失败!
    exit /b 1
)

echo [SUCCESS] 构建完成!
exit /b 0

:show_help
echo Android AAR 构建脚本
echo.
echo 用法: %~nx0 [选项]
echo.
echo 选项:
echo   -e, --env ENV        构建环境 (debug^|release^|release-minified) [默认: debug]
echo   -c, --clean          清理构建缓存
echo   -v, --verbose        详细输出
echo   -h, --help           显示此帮助信息
echo.
echo 示例:
echo   %~nx0                    # 构建 debug 版本
echo   %~nx0 -e release         # 构建 release 版本
echo   %~nx0 -e release-minified -c  # 构建混淆的 release 版本并清理缓存
echo.
exit /b 0
