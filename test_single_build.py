#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试单个APK构建的脚本
用于验证构建流程是否正常工作
"""

import os
import shutil
import subprocess
import sys
from pathlib import Path

# 定义文件映射关系
FILE_MAPPINGS = {
    'config.xml': 'app/src/main/res/values/config.xml',
    'detection_sound.wav': 'app/src/main/assets/res/detection_sound.wav', 
    'ic_launcher.png': 'app/src/main/res/mipmap/ic_launcher.png',
    'splash.png': 'app/src/main/assets/res/splash.png'
}

def test_single_user(user_folder="38"):
    """测试单个用户文件夹的构建流程"""
    print(f"🧪 测试用户文件夹: {user_folder}")
    
    # 检查用户文件夹是否存在
    user_path = Path('userInfos') / user_folder
    if not user_path.exists():
        print(f"❌ 用户文件夹不存在: {user_path}")
        return False
    
    # 检查必需文件
    print("📋 检查必需文件...")
    missing_files = []
    for source_file in FILE_MAPPINGS.keys():
        source_path = user_path / source_file
        if not source_path.exists():
            missing_files.append(str(source_path))
            print(f"⚠️  文件不存在: {source_path}")
        else:
            print(f"✅ 文件存在: {source_path}")
    
    if missing_files:
        print(f"❌ 缺少必需文件，无法继续测试")
        return False
    
    # 备份原始文件
    print("\n💾 备份原始文件...")
    backup_dir = Path('test_backup')
    backup_dir.mkdir(exist_ok=True)
    
    for original_file in FILE_MAPPINGS.values():
        if Path(original_file).exists():
            backup_path = backup_dir / Path(original_file).name
            shutil.copy2(original_file, backup_path)
            print(f"已备份: {original_file} -> {backup_path}")
    
    try:
        # 替换文件
        print(f"\n🔄 替换文件...")
        for source_file, target_file in FILE_MAPPINGS.items():
            source_path = user_path / source_file
            target_path = Path(target_file)
            
            # 确保目标目录存在
            target_path.parent.mkdir(parents=True, exist_ok=True)
            
            # 复制文件
            shutil.copy2(source_path, target_path)
            print(f"✅ 已替换: {source_path} -> {target_path}")
        
        # 测试构建（可选，注释掉以加快测试）
        print(f"\n🏗️  开始构建测试...")
        
        # 清理构建
        print("正在清理构建缓存...")
        gradle_cmd = './gradlew.bat' if os.name == 'nt' else './gradlew'
        result = subprocess.run([gradle_cmd, 'clean'], capture_output=True, text=True)
        
        if result.returncode != 0:
            print(f"❌ 清理失败: {result.stderr}")
            return False
        print("✅ 构建缓存清理完成")
        
        # 构建APK
        print("正在构建release APK...")
        result = subprocess.run([gradle_cmd, 'assembleRelease'], capture_output=True, text=True)
        
        if result.returncode != 0:
            print(f"❌ 构建失败: {result.stderr}")
            return False
        print("✅ APK构建完成")
        
        # 查找生成的APK
        apk_dir = Path('app/build/outputs/apk/release')
        if not apk_dir.exists():
            print(f"❌ APK输出目录不存在: {apk_dir}")
            return False
        
        apk_files = list(apk_dir.glob('*.apk'))
        if not apk_files:
            print("❌ 未找到生成的APK文件")
            return False
        
        apk_path = apk_files[0]
        print(f"✅ 找到APK文件: {apk_path}")
        
        # 复制并重命名APK
        target_apk = Path('userInfos') / f'test_{user_folder}.apk'
        shutil.copy2(apk_path, target_apk)
        print(f"✅ 测试APK已复制到: {target_apk}")
        
        print(f"\n🎉 测试成功！用户 {user_folder} 的APK构建正常")
        return True
        
    except Exception as e:
        print(f"❌ 测试过程中发生错误: {e}")
        return False
        
    finally:
        # 恢复原始文件
        print(f"\n🔄 恢复原始文件...")
        for source_file, target_file in FILE_MAPPINGS.items():
            backup_path = backup_dir / source_file
            if backup_path.exists():
                shutil.copy2(backup_path, target_file)
                print(f"已恢复: {backup_path} -> {target_file}")

def main():
    """主函数"""
    if len(sys.argv) > 1:
        user_folder = sys.argv[1]
    else:
        user_folder = "38"  # 默认测试用户
    
    print("🧪 APK构建流程测试")
    print("=" * 50)
    
    # 检查环境
    if not Path('userInfos').exists():
        print("❌ userInfos目录不存在")
        sys.exit(1)
    
    if not Path('gradlew').exists() and not Path('gradlew.bat').exists():
        print("❌ 未找到gradlew脚本")
        sys.exit(1)
    
    # 运行测试
    success = test_single_user(user_folder)
    
    if success:
        print(f"\n✅ 测试完成！可以运行完整的批量构建脚本了")
        print(f"运行命令: python3 build_multiple_apks.py")
    else:
        print(f"\n❌ 测试失败，请检查配置和环境")

if __name__ == '__main__':
    main() 