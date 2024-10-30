issue_id="issue_moc4331"
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
mysql -h $cn_svc_ip -P 6001 -udump -p111 -e "drop database if exists t;create database t;use t;create table t(id int, id2 int, id3 int);"
mysql -h $cn_1_ip   -P 6001 -udump -p111 -e "select mo_ctl('cn', 'task', 'disable');"
mysql -h $cn_2_ip   -P 6001 -udump -p111 -e "select mo_ctl('cn', 'task', 'disable');"

sleep 5;

echo "insert with empty no index"
./start.sh -c ./prepare_dml/insert  -h ${cn_svc_ip} -P 6001  -t 100 -d 10 -b t > time.log &
/root/jensen/pprof/pprof_collect.sh ${issue_id}_${stats}_disable_insert_with_empty_no_index

mv ${issue_id}_${stats}_disable_insert_with_empty_no_index ${issue_id}/${stats}/

echo "insert with 100w no index"
mysql -h $cn_svc_ip -P 6001 -udump -p111 -e "drop database if exists t;create database t;use t;create table t(id int, id2 int, id3 int);"
sleep 5;
mysql -h $cn_svc_ip -P 6001 -udump -p111 -e "use t;insert into t select result, result, result from generate_series(1,1000000) g;"
sleep 5;

./start.sh -c ./prepare_dml/insert  -h ${cn_svc_ip} -P 6001  -t 100 -d 10 -b t > time.log &
/root/jensen/pprof/pprof_collect.sh ${issue_id}_${stats}_disable_insert_with_100w_no_index

mv ${issue_id}_${stats}_disable_insert_with_100w_no_index ${issue_id}/${stats}/

# ------------------------------------------------------------------------------------
# disable mo_task
mysql -h $cn_svc_ip -P 6001 -udump -p111 -e "drop database if exists t;create database t;use t;create table t(id int, id2 int, id3 int, primary key(id), unique key(id2), key(id3));"

sleep 5;

echo "insert"
./start.sh -c ./prepare_dml/insert  -h ${cn_svc_ip} -P 6001  -t 100 -d 10 -b t > time.log &
/root/jensen/pprof/pprof_collect.sh ${issue_id}_${stats}_disable_insert

mv ${issue_id}_${stats}_disable_insert ${issue_id}/${stats}/

echo "update"
./start.sh -c ./prepare_dml/update  -h ${cn_svc_ip} -P 6001  -t 100 -d 10 -b t > time.log &
/root/jensen/pprof/pprof_collect.sh ${issue_id}_${stats}_disable_update

mv ${issue_id}_${stats}_disable_update ${issue_id}/${stats}/

echo "delete"
./start.sh -c ./prepare_dml/delete  -h ${cn_svc_ip} -P 6001  -t 100 -d 10 -b t > time.log &
/root/jensen/pprof/pprof_collect.sh ${issue_id}_${stats}_disable_delete

mv ${issue_id}_${stats}_disable_delete ${issue_id}/${stats}/

clean-mo-cluster mo-checkin-regression-${namespace}
