#!/bin/bash

# 指定文件路径
file_path="prepare"

# 指定的执行顺序
# script_order=("select_1" "select_star" "select_where" "update" "delete" "insert")
script_order=("select_1")

# 并发量数组
# concurrent_array=(256 512 1024 2048 4096)
concurrent_array=(1 2)

function execute_task(){
    local script_name=$1
    local minute=$2
    local concurrent=$3
    local base=$4

    echo "开始执行:./start.sh -c $file_path/$script_name -d $minute -t $concurrent -b $base"
    start_time=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    echo "开始时间: ${start_time} UTC"

    # 执行具体任务
    ./start.sh -c "$file_path/$script_name" -d $minute -t $concurrent -b $base
    end_time=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    echo 结束时间: ${end_time} UTC""
}

# 修正 for 循环语法
for script in "${script_order[@]}"; do
    for concurrent in "${concurrent_array[@]}"; do
        execute_task "$script" 1 "$concurrent" "jensen"
        # execute_task "$script" 5 "$concurrent" "jensen"
    done
done