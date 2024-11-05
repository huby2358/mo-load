issue_id="issue_"
namespace=""

stats=""

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
mysql -h $cn_2_ip   -P 6001 -udump -p111 -e "select git_version();"

sleep 5;

