issue_id="issue_"
namespace=""

stats="before"

cn_svc_ip=$(kubectl -n mo-checkin-regression-${namespace}  get svc | grep "cn" | grep "6001/TCP"|awk '{print $3}')


# 获取 CNSet 的 Pod IP 地址
cn_ips=$(kubectl -n mo-checkin-regression-${namespace} get pods -l matrixorigin.io/component=CNSet -o=jsonpath='{.items[*].status.podIP}' | sed 's/ /,/g')

# 将 IP 地址分割成数组
IFS=',' read -r -a cn_ip_array <<< "$cn_ips"

cn_1_ip=${cn_ip_array[0]}
cn_2_ip=${cn_ip_array[1]}

# ------------------------------------------------------------------------------------
# disable mo_task

mkdir -p $issue_id
mkdir -p $issue_id/$stats
mysql -h $cn_svc_ip -P 6001 -udump -p111 -e "drop database if exists t;create database t;use t;create table t(id int, id2 int);"
mysql -h $cn_1_ip -P 6001 -udump -p111 -e "select mo_ctl('cn', 'task', 'disable');"
mysql -h $cn_2_ip -P 6001 -udump -p111 -e "select mo_ctl('cn', 'task', 'disable');"
mysql -h $cn_2_ip   -P 6001 -udump -p111 -e "select git_version();"

sleep 5;

echo "select 1"
./start.sh -c ./prepare/select_1 -h ${cn_svc_ip} -P 6001 -t 100 -d 10 -b t > time.log &
/root/jensen/pprof/pprof_collect.sh ${issue_id}_${stats}_disable_select_1 ${cn_1_ip} ${cn_2_ip}

mv ${issue_id}_${stats}_disable_select_1 ${issue_id}/${stats}/

echo "select star"
./start.sh -c ./prepare/select_star -h ${cn_svc_ip} -P 6001 -t 100 -d 10 -b t > time.log &
/root/jensen/pprof/pprof_collect.sh ${issue_id}_${stats}_disable_select_star ${cn_1_ip} ${cn_2_ip}

mv ${issue_id}_${stats}_disable_select_star ${issue_id}/${stats}/

echo "select where"
./start.sh -c ./prepare/select_where -h ${cn_svc_ip} -P 6001 -t 100 -d 10 -b t > time.log &
/root/jensen/pprof/pprof_collect.sh ${issue_id}_${stats}_disable_select_where ${cn_1_ip} ${cn_2_ip}

mv ${issue_id}_${stats}_disable_select_where ${issue_id}/${stats}/

# echo "insert"
# ./start.sh -c ./prepare/insert  -h ${cn_svc_ip} -P 6001  -t 100 -d 10 -b t > time.log &
# /root/jensen/pprof/pprof_collect.sh ${issue_id}_${stats}_disable_insert

# mv ${issue_id}_${stats}_disable_insert ${issue_id}/${stats}/

# echo "update"
# ./start.sh -c ./prepare/update  -h ${cn_svc_ip} -P 6001  -t 100 -d 10 -b t > time.log &
# /root/jensen/pprof/pprof_collect.sh ${issue_id}_${stats}_disable_update

# mv ${issue_id}_${stats}_disable_update ${issue_id}/${stats}/

# echo "delete"
# ./start.sh -c ./prepare/delete  -h ${cn_svc_ip} -P 6001  -t 100 -d 10 -b t > time.log &
# /root/jensen/pprof/pprof_collect.sh ${issue_id}_${stats}_disable_delete

# mv ${issue_id}_${stats}_disable_delete ${issue_id}/${stats}/


clean-mo-cluster mo-checkin-regression-${namespace}
