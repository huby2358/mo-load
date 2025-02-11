#!/bin/bash

# -------------------- NEED FILL --------------------
issue_id="issue_4331"
stats="after" # 填写before or after
is_tke=1 # 如果是本地测试, 需要修改为0

namespace="mo-checkin-regression-12113815979"
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
    cn_2_ip=${cn_ip_array[1]}
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
mysql -h $cn_2_ip -P 6001 -udump -p111 -e "select mo_ctl('cn', 'task', 'disable');"
mysql -h $cn_2_ip -P 6001 -udump -p111 -e "select git_version();"

sleep 5

# -------------------------------------------------------------------------------------
#                                       QUERY
# -------------------------------------------------------------------------------------

# ------------------------------------------------------------------------------------

echo "select 1"

./start.sh -c ./prepare/select_1 -h ${cn_svc_ip} -P 6001 -t 100 -d 10 -b t >time.log &
./pprof_collect.sh ${issue_id}_${stats}_select_1 ${cn_1_ip} ${cn_2_ip} ${collectPort}
mv ${issue_id}_${stats}_select_1 ${issue_id}/${stats}/

# ------------------------------------------------------------------------------------

echo "select star"

./start.sh -c ./prepare/select_star -h ${cn_svc_ip} -P 6001 -t 100 -d 10 -b t >time.log &
./pprof_collect.sh ${issue_id}_${stats}_select_star ${cn_1_ip} ${cn_2_ip} ${collectPort}
mv ${issue_id}_${stats}_select_star ${issue_id}/${stats}/

# ------------------------------------------------------------------------------------

echo "select where"

./start.sh -c ./prepare/select_where -h ${cn_svc_ip} -P 6001 -t 100 -d 10 -b t >time.log &
./pprof_collect.sh ${issue_id}_${stats}_select_where ${cn_1_ip} ${cn_2_ip} ${collectPort}
mv ${issue_id}_${stats}_select_where ${issue_id}/${stats}/

# -------------------------------------------------------------------------------------
#                                       DML
# -------------------------------------------------------------------------------------

echo "insert with empty no index"

mysql -h $cn_svc_ip -P 6001 -udump -p111 -e "drop database if exists t;create database t;use t;create table t(id int, id2 int, id3 int);"
sleep 1

./start.sh -c ./prepare/insert -h ${cn_svc_ip} -P 6001 -t 100 -d 10 -b t >time.log &
./pprof_collect.sh ${issue_id}_${stats}_insert_with_empty_no_index ${cn_1_ip} ${cn_2_ip} ${collectPort}
mv ${issue_id}_${stats}_insert_with_empty_no_index ${issue_id}/${stats}/

# ------------------------------------------------------------------------------------

echo "insert with 100w no index"

# prepare data
mysql -h $cn_svc_ip -P 6001 -udump -p111 -e "drop database if exists t;create database t;use t;create table t(id int, id2 int, id3 int);"
sleep 1
mysql -h $cn_svc_ip -P 6001 -udump -p111 -e "use t;insert into t select result, result, result from generate_series(1,1000000) g;"
sleep 1

./start.sh -c ./prepare/insert -h ${cn_svc_ip} -P 6001 -t 100 -d 10 -b t >time.log &
./pprof_collect.sh ${issue_id}_${stats}_insert_with_100w_no_index ${cn_1_ip} ${cn_2_ip} ${collectPort}
mv ${issue_id}_${stats}_insert_with_100w_no_index ${issue_id}/${stats}/

# ------------------------------------------------------------------------------------

echo "insert with empty contains indexes"

mysql -h $cn_svc_ip -P 6001 -udump -p111 -e "drop database if exists t;create database t;use t;create table t(id int, id2 int, id3 int, primary key(id), unique key(id2), key(id3));"
sleep 1

./start.sh -c ./prepare/insert -h ${cn_svc_ip} -P 6001 -t 100 -d 10 -b t >time.log &
./pprof_collect.sh ${issue_id}_${stats}_insert_with_empty_indexes ${cn_1_ip} ${cn_2_ip} ${collectPort}
mv ${issue_id}_${stats}_insert_with_empty_indexes ${issue_id}/${stats}/

# ------------------------------------------------------------------------------------

echo "insert with 100w contains indexes"

# prepare data
mysql -h $cn_svc_ip -P 6001 -udump -p111 -e "drop database if exists t;create database t;use t;create table t(id int, id2 int, id3 int, primary key(id), unique key(id2), key(id3));"
sleep 1
mysql -h $cn_svc_ip -P 6001 -udump -p111 -e "use t;insert into t select result, result, result from generate_series(1,1000000) g;"
sleep 5

./start.sh -c ./prepare/insert -h ${cn_svc_ip} -P 6001 -t 100 -d 10 -b t >time.log &
./pprof_collect.sh ${issue_id}_${stats}_insert_with_100w_indexes ${cn_1_ip} ${cn_2_ip} ${collectPort}
mv ${issue_id}_${stats}_insert_with_100w_indexes ${issue_id}/${stats}/

# ------------------------------------------------------------------------------------

echo "update"

./start.sh -c ./prepare/update -h ${cn_svc_ip} -P 6001 -t 100 -d 10 -b t >time.log &
./pprof_collect.sh ${issue_id}_${stats}_update ${cn_1_ip} ${cn_2_ip} ${collectPort}
mv ${issue_id}_${stats}_update ${issue_id}/${stats}/

# ------------------------------------------------------------------------------------

echo "delete"

./start.sh -c ./prepare/delete -h ${cn_svc_ip} -P 6001 -t 100 -d 10 -b t >time.log &
./pprof_collect.sh ${issue_id}_${stats}_delete ${cn_1_ip} ${cn_2_ip} ${collectPort}
mv ${issue_id}_${stats}_delete ${issue_id}/${stats}/

# ------------------------------------------------------------------------------------

if [ $is_tke -eq 1 ]; then
    clean-mo-cluster mo-checkin-regression-${namespace}
fi
