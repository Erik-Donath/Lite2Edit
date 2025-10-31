@echo off
setlocal enabledelayedexpansion

echo ========================================
echo Building Lite2Edit for all MC versions
echo ========================================

:: Create output directory for all builds
if not exist "builds" mkdir builds

:: Define all supported Minecraft versions
set versions=1.20.2 1.20.4 1.20.5 1.20.6 1.21 1.21.1 1.21.3 1.21.4 1.21.5 1.21.6 1.21.7 1.21.8 1.21.9 1.21.10

:: Counter for successful builds
set /a success_count=0
set /a total_count=0

:: Build for each version
for %%v in (%versions%) do (
    set /a total_count+=1
    echo.
    echo ========================================
    echo Building for Minecraft %%v
    echo ========================================

    :: Clean previous build
    call gradlew clean

    :: Build with specific MC version
    call gradlew build "-Pminecraft_version=%%v"

    :: Check if build was successful
    if !errorlevel! equ 0 (
        echo [SUCCESS] Build completed for Minecraft %%v
        set /a success_count+=1

        :: Copy the built JAR to builds directory with version-specific name
        if exist "build\libs\Lite2Edit-0.1.jar" (
            copy "build\libs\Lite2Edit-0.1.jar" "builds\lite2edit-fabric-%%v-0.1.jar"
            echo [COPIED] lite2edit-fabric-%%v-0.1.jar
        ) else (
            echo [WARNING] JAR file not found for %%v
        )
    ) else (
        echo [FAILED] Build failed for Minecraft %%v
    )
)

echo.
echo ========================================
echo Build Summary
echo ========================================
echo Total versions: !total_count!
echo Successful builds: !success_count!
echo Failed builds: !total_count! - !success_count! =
set /a failed_count=!total_count!-!success_count!
echo !failed_count!
echo.
echo All builds completed!
echo Check the 'builds' directory for output files.
pause
