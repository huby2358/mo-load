#!/bin/bash

# -------------------- NEED FILL --------------------
issue_id="issue_4331_10"
stats="after" # 填写before or after
is_tke=1 # 如果是本地测试, 需要修改为0

namespace="12272357515"
collectPort=6060

# -------------------- NEED FILL --------------------

# 本地采集, 默认使用本地的ip地址
cn_svc_ip=127.0.0.1
cn_1_ip=127.0.0.1
cn_2_ip=127.0.0.1


if [ $is_tke -eq 0 ]; then
    echo "在本地进行prepare测试"
    
else
    if [ -z "$namespace" ]; then
        echo "tke上需要namespace的信息来获取cn的ip地址"
        exit 1
    fi

    echo "基于namespace获取cn的ip地址"

    cn_svc_ip=$(kubectl -n mo-checkin-regression-${namespace} get svc | grep "cn" | grep "6001/TCP" | awk '{print $3}')
    # 获取 CNSet 的 Pod IP 地址
    cn_ips=$(kubectl -n mo-checkin-regression-${namespace} get pods -l matrixorigin.io/component=CNSet -o=jsonpath='{.items[*].status.podIP}' | sed 's/ /,/g')
    # 将 IP 地址分割成数组
    IFS=',' read -r -a cn_ip_array <<<"$cn_ips"
    cn_1_ip=${cn_ip_array[0]}
   # cn_2_ip=${cn_ip_array[1]}
fi

if [ -z "$stats" ]; then
    echo "请填写当前测试的分支的状态, 是修改前请填写(before), 修改后请填写(after)"
    exit 1
fi

if [[ $issue_id =~ ^issue_[0-9]+$ || $issue_id =~ ^issue_moc[0-9]+$ ]]; then
    echo "test for issue_id: $issue_id with stats: $stats"
else
    echo "请填写正确的issue_id"
    exit 1
fi

# disable mo_task
mkdir -p $issue_id
mkdir -p $issue_id/$stats
mysql -h $cn_svc_ip -P 6001 -udump -p111 -e "drop database if exists t;create database t;use t;create table t(id int, id2 int, id3 int);"
mysql -h $cn_1_ip -P 6001 -udump -p111 -e "select mo_ctl('cn', 'task', 'disable');"
#mysql -h $cn_2_ip -P 6001 -udump -p111 -e "select mo_ctl('cn', 'task', 'disable');"
mysql -h $cn_1_ip -P 6001 -udump -p111 -e "select git_version();"

sleep 5

# -------------------------------------------------------------------------------------
#                                       QUERY
# -------------------------------------------------------------------------------------

# ------------------------------------------------------------------------------------


# -------------------------------------------------------------------------------------
#                                       DML
# -------------------------------------------------------------------------------------

echo "insert with empty no index"

mysql -h $cn_svc_ip -P 6001 -udump -p111 -e "drop database if exists t;create database t;use t;create table t(id int, id2 int, id3 int);"
sleep 1

./start.sh -c ./prepare/insert -h ${cn_svc_ip} -P 6001 -t 10 -d 5 -b t >time.log &
./pprof_collect.sh ${issue_id}_${stats}_insert_with_empty_no_index ${cn_1_ip} ${cn_2_ip} ${collectPort}
mv ${issue_id}_${stats}_insert_with_empty_no_index ${issue_id}/${stats}/

# ------------------------------------------------------------------------------------



# ------------------------------------------------------------------------------------



# ------------------------------------------------------------------------------------


# ------------------------------------------------------------------------------------


# ------------------------------------------------------------------------------------


# ------------------------------------------------------------------------------------

if [ $is_tke -eq 1 ]; then
    clean-mo-cluster mo-checkin-regression-${namespace}
fi
