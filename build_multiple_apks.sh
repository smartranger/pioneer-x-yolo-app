#!/bin/bash

# 自动构建多个APK的脚本
# 根据userInfos文件夹中的配置，为每个配置生成独立的APK文件

set -e  # 遇到错误时退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 文件映射关系
declare -A FILE_MAPPINGS=(
    ["config.xml"]="app/src/main/res/values/config.xml"
    ["detection_sound.wav"]="app/src/main/assets/res/detection_sound.wav"
    ["ic_launcher.png"]="app/src/main/res/mipmap/ic_launcher.png"
    ["splash.png"]="app/src/main/assets/res/splash.png"
)

# 创建备份目录
BACKUP_DIR="backup_original"

# 函数：打印带颜色的信息
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

# 函数：备份原始文件
backup_original_files() {
    print_info "正在备份原始文件..."
    mkdir -p "$BACKUP_DIR"
    
    for file in "${FILE_MAPPINGS[@]}"; do
        if [[ -f "$file" ]]; then
            filename=$(basename "$file")
            cp "$file" "$BACKUP_DIR/$filename"
            print_info "已备份: $file -> $BACKUP_DIR/$filename"
        fi
    done
}

# 函数：恢复原始文件
restore_original_files() {
    print_info "正在恢复原始文件..."
    
    if [[ ! -d "$BACKUP_DIR" ]]; then
        print_warning "备份目录不存在，无法恢复原始文件"
        return
    fi
    
    for source_file in "${!FILE_MAPPINGS[@]}"; do
        target_file="${FILE_MAPPINGS[$source_file]}"
        backup_file="$BACKUP_DIR/$source_file"
        
        if [[ -f "$backup_file" ]]; then
            cp "$backup_file" "$target_file"
            print_info "已恢复: $backup_file -> $target_file"
        fi
    done
}

# 函数：替换文件
replace_files() {
    local user_folder=$1
    local user_path="userInfos/$user_folder"
    
    if [[ ! -d "$user_path" ]]; then
        print_error "用户文件夹不存在: $user_path"
        return 1
    fi
    
    for source_file in "${!FILE_MAPPINGS[@]}"; do
        target_file="${FILE_MAPPINGS[$source_file]}"
        source_path="$user_path/$source_file"
        
        if [[ ! -f "$source_path" ]]; then
            print_warning "源文件不存在: $source_path"
            continue
        fi
        
        # 确保目标目录存在
        mkdir -p "$(dirname "$target_file")"
        
        # 复制文件
        cp "$source_path" "$target_file"
        print_info "已替换: $source_path -> $target_file"
    done
    
    return 0
}

# 函数：清理构建
clean_build() {
    print_info "正在清理构建缓存..."
    if ./gradlew clean; then
        print_success "构建缓存清理完成"
        return 0
    else
        print_error "清理构建缓存失败"
        return 1
    fi
}

# 函数：构建release APK
build_release_apk() {
    print_info "正在构建release APK..."
    if ./gradlew assembleRelease; then
        print_success "APK构建完成"
        return 0
    else
        print_error "APK构建失败"
        return 1
    fi
}

# 函数：查找生成的APK文件
find_generated_apk() {
    local apk_dir="app/build/outputs/apk/release"
    
    if [[ ! -d "$apk_dir" ]]; then
        print_error "APK输出目录不存在: $apk_dir"
        return 1
    fi
    
    # 查找APK文件
    local apk_file=$(find "$apk_dir" -name "*.apk" -type f | head -n 1)
    
    if [[ -z "$apk_file" ]]; then
        print_error "未找到生成的APK文件"
        return 1
    fi
    
    echo "$apk_file"
    return 0
}

# 函数：复制并重命名APK文件
copy_and_rename_apk() {
    local apk_path=$1
    local user_folder=$2
    local target_apk="userInfos/${user_folder}.apk"
    
    if [[ ! -f "$apk_path" ]]; then
        print_error "APK文件不存在: $apk_path"
        return 1
    fi
    
    cp "$apk_path" "$target_apk"
    print_success "APK已复制到: $target_apk"
    return 0
}

# 函数：处理单个用户文件夹
process_user_folder() {
    local user_folder=$1
    
    echo
    echo "=================================================="
    print_info "正在处理用户文件夹: $user_folder"
    echo "=================================================="
    
    # 1. 替换文件
    if ! replace_files "$user_folder"; then
        print_error "替换文件失败，跳过用户: $user_folder"
        return 1
    fi
    
    # 2. 清理构建
    if ! clean_build; then
        print_error "清理构建失败，跳过用户: $user_folder"
        return 1
    fi
    
    # 3. 构建APK
    if ! build_release_apk; then
        print_error "构建APK失败，跳过用户: $user_folder"
        return 1
    fi
    
    # 4. 查找生成的APK
    local apk_path
    if ! apk_path=$(find_generated_apk); then
        print_error "未找到APK文件，跳过用户: $user_folder"
        return 1
    fi
    
    # 5. 复制并重命名APK
    if ! copy_and_rename_apk "$apk_path" "$user_folder"; then
        print_error "复制APK失败，跳过用户: $user_folder"
        return 1
    fi
    
    print_success "✅ 用户 $user_folder 处理完成"
    return 0
}

# 主函数
main() {
    echo "🚀 开始批量构建APK..."
    
    # 检查必要的目录和文件
    if [[ ! -d "userInfos" ]]; then
        print_error "userInfos目录不存在"
        exit 1
    fi
    
    if [[ ! -f "gradlew" ]]; then
        print_error "未找到gradlew脚本"
        exit 1
    fi
    
    # 确保gradlew可执行
    chmod +x gradlew
    
    # 备份原始文件
    backup_original_files
    
    # 获取所有用户文件夹
    local user_folders=()
    while IFS= read -r -d '' folder; do
        local folder_name=$(basename "$folder")
        # 跳过隐藏文件夹
        if [[ ! "$folder_name" =~ ^\. ]]; then
            user_folders+=("$folder_name")
        fi
    done < <(find userInfos -maxdepth 1 -type d -not -path userInfos -print0)
    
    if [[ ${#user_folders[@]} -eq 0 ]]; then
        print_error "未找到任何用户文件夹"
        exit 1
    fi
    
    print_info "找到 ${#user_folders[@]} 个用户文件夹: ${user_folders[*]}"
    
    # 处理每个用户文件夹
    local success_count=0
    local failed_folders=()
    
    # 设置trap确保总是恢复原始文件
    trap 'restore_original_files' EXIT
    
    for user_folder in "${user_folders[@]}"; do
        if process_user_folder "$user_folder"; then
            ((success_count++))
        else
            failed_folders+=("$user_folder")
        fi
    done
    
    # 输出结果
    echo
    echo "============================================================"
    print_info "📊 批量构建完成!"
    print_success "✅ 成功构建: $success_count 个APK"
    print_error "❌ 失败数量: ${#failed_folders[@]} 个"
    
    if [[ ${#failed_folders[@]} -gt 0 ]]; then
        print_error "失败的文件夹: ${failed_folders[*]}"
    fi
    
    echo "============================================================"
}

# 脚本入口
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi 