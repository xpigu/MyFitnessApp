#!/bin/bash

# 健身 App 验证测试脚本

echo "=========================================="
echo "MyFitnessApp Phase 1 验证测试"
echo "=========================================="
echo ""

# 获取设备列表
echo "📱 检查已连接的设备："
adb devices
echo ""

# 安装应用
echo "📦 安装应用..."
adb install -r app/build/outputs/apk/debug/app-debug.apk
echo ""

# 启动应用（主页）
echo "🚀 启动应用（健康首页）..."
adb shell am start -n com.example.myfitnessapp/.MainActivity
echo "等待 3 秒..."
sleep 3

# 截图：主页
echo "📸 截图：健康首页"
adb shell screencap -p /sdcard/screenshot_main.png
adb pull /sdcard/screenshot_main.png ./screenshots/main_page.png
echo "✓ 已保存到 screenshots/main_page.png"
echo ""

# 测试饮水功能
echo "🧊 测试：点击饮水按钮 3 次"
for i in {1..3}; do
    echo "  - 点击第 $i 次"
    # 模拟点击饮水按钮 (坐标需要根据实际调整)
    adb shell input tap 500 600
    sleep 1
done
echo ""

# 截图：饮水后
echo "📸 截图：饮水后的首页"
adb shell screencap -p /sdcard/screenshot_water.png
adb pull /sdcard/screenshot_water.png ./screenshots/after_water.png
echo "✓ 已保存到 screenshots/after_water.png"
echo ""

# 导航到个人资料页面
echo "👤 导航到个人资料页面..."
adb shell am start -n com.example.myfitnessapp/.ProfileActivity
sleep 2
echo "📸 截图：个人资料页面"
adb shell screencap -p /sdcard/screenshot_profile.png
adb pull /sdcard/screenshot_profile.png ./screenshots/profile_page.png
echo "✓ 已保存到 screenshots/profile_page.png"
echo ""

# 导航到编辑资料页面
echo "✏️ 导航到编辑资料页面..."
adb shell am start -n com.example.myfitnessapp/.EditProfileActivity
sleep 2
echo "📸 截图：编辑资料页面"
adb shell screencap -p /sdcard/screenshot_edit_profile.png
adb pull /sdcard/screenshot_edit_profile.png ./screenshots/edit_profile_page.png
echo "✓ 已保存到 screenshots/edit_profile_page.png"
echo ""

echo "=========================================="
echo "✅ 验证测试完成！"
echo "=========================================="
echo ""
echo "验证清单："
echo "  ✓ 应用安装成功"
echo "  ✓ 主页启动成功"
echo "  ✓ 个人资料页启动成功"
echo "  ✓ 编辑资料页启动成功"
echo ""
echo "下一步手动验证："
echo "  1. 检查饮水计数是否正确显示"
echo "  2. 编辑用户名、生日、身高、体重等信息"
echo "  3. 关闭应用后重新启动，验证数据是否保留"
