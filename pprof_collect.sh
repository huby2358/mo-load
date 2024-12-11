#!/bin/bash

# called by prepare_test.sh, don't use it directly

# 检查是否提供了参数
if [ $# -eq 0 ]; then
    echo "Usage: $0 <folder_name>"
    exit 1
fi

folder_name=$1 # 将第一个参数赋值给folder_name变量
cn1=$2
cn2=$3
collectPort=$4

count=1

# 定义一个函数，用于处理ctrl-c信号
function cleanup() {
    echo "Exiting..."
    # 执行需要在接收到ctrl-c信号时执行的命令
    # 例如，可以在这里进行清理工作或退出脚本
    exit 0
}

# 设置trap命令，捕获ctrl-c信号并调用cleanup函数
trap cleanup SIGINT

# 检查文件夹是否存在
if [ -d "$folder_name" ]; then
    echo "Folder '$folder_name' exists, removing it..."
    rm -rf "$folder_name"
fi

# 创建文件夹
mkdir "$folder_name"

# 保存当前目录
# current_dir=$(pwd)

start_time=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
echo "开始时间: ${start_time} UTC"

while true; do
    # 使用$folder_name变量来构建文件路径

    # cn1
    curl -o "$folder_name/cn_1_${folder_name}_${count}_cpu.out" http://${cn1}:${collectPort}/debug/pprof/profile?seconds=30 >/dev/null 2>&1 &
    curl -o "$folder_name/cn_1_${folder_name}_${count}_heap.out" http://${cn1}:${collectPort}/debug/pprof/heap >/dev/null 2>&1 &

    # cn2
   # curl -o "$folder_name/cn_2_${folder_name}_${count}_cpu.out" http://${cn2}:${collectPort}/debug/pprof/profile?seconds=30 >/dev/null 2>&1 &
    # curl -o "$folder_name/cn_2_${folder_name}_${count}_heap.out" http://${cn2}:${collectPort}/debug/pprof/heap >/dev/null 2>&1 &

    sleep 30

    # 更新计数器
    count=$((count + 1))

    if [ $count -gt 3 ]; then
        echo "采集完成"
        break
    fi

done

sleep 180

mv ./time.log ${folder_name}/

end_time=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
echo 结束时间: "${end_time} UTC"
