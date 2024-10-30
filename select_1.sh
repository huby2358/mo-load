./start.sh -c prepare/select_1 -h 127.0.0.1 -P 6001 -t 100 -d 3 -b t > prepare_select_1.log

tail -n 20 prepare_select_1.log

./start.sh -c prepare/select_1_normal -h 127.0.0.1 -P 6001 -t 100 -d 3 -b t > normal_select_1.log

tail -n 20 normal_select_1.log
