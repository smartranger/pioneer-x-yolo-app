#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
自动构建多个APK的脚本
根据userInfos文件夹中的配置，为每个配置生成独立的APK文件
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

def backup_original_files():
    """备份原始文件"""
    backup_dir = Path('backup_original')
    backup_dir.mkdir(exist_ok=True)
    
    for original_file in FILE_MAPPINGS.values():
        if Path(original_file).exists():
            backup_path = backup_dir / Path(original_file).name
            shutil.copy2(original_file, backup_path)
            print(f"已备份: {original_file} -> {backup_path}")

def restore_original_files():
    """恢复原始文件"""
    backup_dir = Path('backup_original')
    if not backup_dir.exists():
        print("警告: 备份目录不存在，无法恢复原始文件")
        return
    
    for source_file, target_file in FILE_MAPPINGS.items():
        backup_path = backup_dir / source_file
        if backup_path.exists():
            shutil.copy2(backup_path, target_file)
            print(f"已恢复: {backup_path} -> {target_file}")

def replace_files(user_folder):
    """替换项目中的资源文件"""
    user_path = Path('userInfos') / user_folder
    
    if not user_path.exists():
        print(f"错误: 用户文件夹不存在: {user_path}")
        return False
    
    for source_file, target_file in FILE_MAPPINGS.items():
        source_path = user_path / source_file
        target_path = Path(target_file)
        
        if not source_path.exists():
            print(f"警告: 源文件不存在: {source_path}")
            continue
            
        # 确保目标目录存在
        target_path.parent.mkdir(parents=True, exist_ok=True)
        
        # 复制文件
        shutil.copy2(source_path, target_path)
        print(f"已替换: {source_path} -> {target_path}")
    
    return True

def clean_build():
    """清理构建缓存"""
    print("正在清理构建缓存...")
    try:
        # 在Windows上使用gradlew.bat，在Unix系统上使用gradlew
        gradle_cmd = './gradlew.bat' if os.name == 'nt' else './gradlew'
        subprocess.run([gradle_cmd, 'clean'], check=True, cwd='.')
        print("构建缓存清理完成")
        return True
    except subprocess.CalledProcessError as e:
        print(f"清理构建缓存失败: {e}")
        return False

def build_release_apk():
    """构建release版本APK"""
    print("正在构建release APK...")
    try:
        # 在Windows上使用gradlew.bat，在Unix系统上使用gradlew
        gradle_cmd = './gradlew.bat' if os.name == 'nt' else './gradlew'
        subprocess.run([gradle_cmd, 'assembleRelease'], check=True, cwd='.')
        print("APK构建完成")
        return True
    except subprocess.CalledProcessError as e:
        print(f"APK构建失败: {e}")
        return False

def find_generated_apk():
    """查找生成的APK文件"""
    apk_dir = Path('app/build/outputs/apk/release')
    if not apk_dir.exists():
        print(f"错误: APK输出目录不存在: {apk_dir}")
        return None
    
    # 查找APK文件
    apk_files = list(apk_dir.glob('*.apk'))
    if not apk_files:
        print("错误: 未找到生成的APK文件")
        return None
    
    # 返回第一个APK文件
    return apk_files[0]

def copy_and_rename_apk(apk_path, user_folder):
    """复制并重命名APK文件到userInfos目录"""
    if not apk_path or not apk_path.exists():
        print(f"错误: APK文件不存在: {apk_path}")
        return False
    
    target_apk = Path('userInfos') / f'{user_folder}.apk'
    shutil.copy2(apk_path, target_apk)
    print(f"APK已复制到: {target_apk}")
    return True

def process_user_folder(user_folder):
    """处理单个用户文件夹"""
    print(f"\n{'='*50}")
    print(f"正在处理用户文件夹: {user_folder}")
    print(f"{'='*50}")
    
    # 1. 替换文件
    if not replace_files(user_folder):
        print(f"替换文件失败，跳过用户: {user_folder}")
        return False
    
    # 2. 清理构建
    if not clean_build():
        print(f"清理构建失败，跳过用户: {user_folder}")
        return False
    
    # 3. 构建APK
    if not build_release_apk():
        print(f"构建APK失败，跳过用户: {user_folder}")
        return False
    
    # 4. 查找生成的APK
    apk_path = find_generated_apk()
    if not apk_path:
        print(f"未找到APK文件，跳过用户: {user_folder}")
        return False
    
    # 5. 复制并重命名APK
    if not copy_and_rename_apk(apk_path, user_folder):
        print(f"复制APK失败，跳过用户: {user_folder}")
        return False
    
    print(f"✅ 用户 {user_folder} 处理完成")
    return True

def main():
    """主函数"""
    print("🚀 开始批量构建APK...")
    
    # 检查必要的目录和文件
    if not Path('userInfos').exists():
        print("错误: userInfos目录不存在")
        sys.exit(1)
    
    if not Path('gradlew').exists() and not Path('gradlew.bat').exists():
        print("错误: 未找到gradlew脚本")
        sys.exit(1)
    
    # 备份原始文件
    print("正在备份原始文件...")
    backup_original_files()
    
    # 获取所有用户文件夹
    userinfos_path = Path('userInfos')
    user_folders = [f.name for f in userinfos_path.iterdir() 
                   if f.is_dir() and not f.name.startswith('.')]
    
    if not user_folders:
        print("错误: 未找到任何用户文件夹")
        sys.exit(1)
    
    print(f"找到 {len(user_folders)} 个用户文件夹: {user_folders}")
    
    # 处理每个用户文件夹
    success_count = 0
    failed_folders = []
    
    try:
        for user_folder in user_folders:
            try:
                if process_user_folder(user_folder):
                    success_count += 1
                else:
                    failed_folders.append(user_folder)
            except Exception as e:
                print(f"处理用户 {user_folder} 时发生异常: {e}")
                failed_folders.append(user_folder)
    
    finally:
        # 恢复原始文件
        print("\n正在恢复原始文件...")
        restore_original_files()
    
    # 输出结果
    print(f"\n{'='*60}")
    print(f"📊 批量构建完成!")
    print(f"✅ 成功构建: {success_count} 个APK")
    print(f"❌ 失败数量: {len(failed_folders)} 个")
    
    if failed_folders:
        print(f"失败的文件夹: {failed_folders}")
    
    print(f"{'='*60}")

if __name__ == '__main__':
    main() 