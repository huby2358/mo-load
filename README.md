
如果要在本地运行`prepare`的子tasks, 请follow 如下指令, 在这之前, 请确保本地已经运行了matrixone, 并且你的环境能正确使用mo-load.

1. 打开prepare_test.sh, 其中有如下内容, 这里的每一项都需要手动填写
```sh
issue_id="issue_"
stats="" # 填写before or after
is_tke=0 # 如果是本地测试, 需要修改为0

namespace=""
collectPort=6060
```

- 假设你在本地对比的是issue_1完成前后的性能差异, 那么`issue_id=issue_1`, `is_tke=0`不需要进行修改
  - 第一次基于修改前的代码起mo时, 设置`stats=before`, 
  - 第二次基于修改后的代码起mo时, 设置`stats=after`
  - namespace在本地跑的时候, 不需要进行设置
  - collectPort在本地跑的时候, 需要与debug-http的端口一致, 比如我平时习惯用9876, 那么这里就填写`collectPort=9876`, tke上的默认配置是6060
    ```sh
    ./mo-service -debug-http :9876 -launch ./etc/launch/launch.toml  > mo-service.log 2>&1 &
    ```

- 测试流程说明, 依次执行下面所有任务, 每个任务跑10分钟, 100并发
  1. select 1
  2. select star
  3. select where
  4. insert with empty no index
  5. insert with 100w no index
  6. insert with empty contains indexes
  7. insert with 100w contains indexe
  8. update
  9. delete

目前, 如果不想跑所有的流程, 需要手动把其他的流程给注释掉, 在`prepare_test.sh`中, 已经用`# ----`的方式对每个流程进行了分割, 可以根据需要进行注释

e.g. 下面这种方式, 就把`select star`的流程给注释掉了
- 彭振哥需要注意, 跑QUERY的时候, **应该是需要把DML的所有流程给注释掉**
```
# -------------------------------------------------------------------------------------
#                                       QUERY
# -------------------------------------------------------------------------------------

# ------------------------------------------------------------------------------------

echo "select 1"

./start.sh -c ./prepare/select_1 -h ${cn_svc_ip} -P 6001 -t 100 -d 2 -b t >time.log &
./pprof_collect.sh ${issue_id}_${stats}_select_1 ${cn_1_ip} ${cn_2_ip} ${collectPort}
mv ${issue_id}_${stats}_select_1 ${issue_id}/${stats}/

# ------------------------------------------------------------------------------------

# echo "select star"
# 
# ./start.sh -c ./prepare/select_star -h ${cn_svc_ip} -P 6001 -t 100 -d 2 -b t >time.log &
# ./pprof_collect.sh ${issue_id}_${stats}_select_star ${cn_1_ip} ${cn_2_ip} ${collectPort}
# mv ${issue_id}_${stats}_select_star ${issue_id}/${stats}/

# ------------------------------------------------------------------------------------

echo "select where"

./start.sh -c ./prepare/select_where -h ${cn_svc_ip} -P 6001 -t 100 -d 2 -b t >time.log &
./pprof_collect.sh ${issue_id}_${stats}_select_where ${cn_1_ip} ${cn_2_ip} ${collectPort}
mv ${issue_id}_${stats}_select_where ${issue_id}/${stats}/

# -------------------------------------------------------------------------------------
#                                       DML
# -------------------------------------------------------------------------------------
```

跑完之后, 会在当前目录下生成一个`issue_1`的文件夹, 里面包含了所有的测试结果, 以及pprof的数据, 格式参阅[这里](https://github.com/matrixorigin/docs/wiki/Prepare%E6%80%A7%E8%83%BD%E8%B7%9F%E8%B8%AA%E7%9A%84%E6%B5%8B%E8%AF%95%E8%AF%B4%E6%98%8E#03-%E6%B5%8B%E8%AF%95%E6%96%87%E4%BB%B6%E6%A0%BC%E5%BC%8F%E8%AF%B4%E6%98%8E)

pprof文件需要自己打开手动解析, tps的对比, 可以利用`make_excel.py`下的脚本, 请确保使用的python环境有`pandas`库, 这个py文件头部有一个路径需要设置, 设置完成后执行即可, 在当前路径下生成一个md表格.
```py
directory_path = "./issue_"
```

NOTE : 本地现在只支持单个节点的测试, 如果是`tke`环境(`is_tke=1`),脚本会自动根据`namespace`去获取`cn_1_ip`和`cn_2_ip`. 在本地测试的时候, `cn_svc_ip`, `cn_1_ip`, `cn_2_ip`实际上是一样的. 脚本目前还不支持本地起多CN去测. 

```sh
# 本地采集, 默认使用本地的ip地址
cn_svc_ip=127.0.0.1
cn_1_ip=127.0.0.1
cn_2_ip=127.0.0.1
```



*******


# What's in MO-Load?

MO-Load is a java-based perforamce test tool for MatrixOne.
It supports users to customize transaction scenarios and concurrency through configuration, and provides multiple types of variable definitions.
Users can reference variables in transaction definitions to make transaction definitions more flexible and closer to real scenarios.



# How to use MO-Load?

## 1. Prepare the testing environment

* Make sure you have installed jdk8.

* Launch MatrixOne or other database instance. Please refer to more information about [how to install and launch MatrixOne](https://github.com/matrixorigin/matrixorigin.io/blob/main/docs/MatrixOne/Get-Started/install-standalone-matrixone.md).

* Clone *mo-load* repository.

  ```
  git clone https://github.com/matrixorigin/mo-load.git
  ```

* Clone *matrixOne* repository.

   ```
   git clone https://github.com/matrixorigin/matrixone.git
   ```

## 2. Configure `Mo-Load`

* In `mo.yml` file, configure the server address, default database name, username, and password, etc. MO-Load is based on java, so these parameters are required for the JDBC(JDBC，Java Database Connectivity) driver. Below is a default example for a local standalone version MatrixOne.

  ```
  jdbc:
    driver: "com.mysql.cj.jdbc.Driver"
    server:
    - addr: "127.0.0.1:6001"
    database:
      default: "test"
    paremeter:
      characterEncoding: "UTF-8"
      useUnicode: "true"
      autoReconnect: "true"
      continueBatchOnError: "false"
      useServerPrepStmts: "true"
      alwaysSendSetIsolation: "false"
      useLocalSessionState: "true"
      zeroDateTimeBehavior: "CONVERT_TO_NULL"
      failoverReadOnly: "false"
      serverTimezone: "Asia/Shanghai"
      socketTimeout: 10000
  user:
    name: "dump"
    passwrod: "111"
  ```
* In `run.yml` file, define transaction related information, such as SQL statements, transaction name, execution duration, concurrency, etc. Below is a example.

  ```
  #Test process data output to:
  #file: only to log file
  #console: to both log file and console
  stdout: "file"
  
  #Execution time of all transactions, unit minute
  duration: 1

  transaction:
  - name: "point_select" #Transaction name

  #The concurrency of executing the transaction test
  vuser: 5
  
  #Execution mode:
  #0 indicates that the sql in the script is executed directly and sequentially. 
  #1 indicates that the sql in the script is encapsulated into a database transaction for execution
  mode: 0
  
  #Whether it is necessary to prepare the sql of the script. 
  #If it is true, there must be only one sql in the script, otherwise this parameter will become invalid
  prepared: "false"
  
  #SQL statements of transaction, which can be multiple
  #Among them, the content enclosed by {} is the referenced variable, which will be replaced with the value of the specific variable during execution
  script:
  - sql: "select k from sbtest_0 where id = {id};"
  ```


* In `replace.yml` file, define variables referenced in `run.yml`. 

  Currently, mo load provides three user-defined type variables and eight built-in variables

  Definitions of three user-defined types of variables, as shown in the following example.

  ```
  replace:
  - name: id         #variable name, and will be referenced in `run.yml` using {}
    type: sequence   #type sequence
    start: 100000    #value that this sequence starts from
    step: 1          #step that this sequence value will increase by for each transaction-run
  
  - name: price      #variable name, and will be referenced in `run.yml` using {}
    type: random     #type random
    range: 1,1000    #random value range
  
  - name: name       #variable name, and will be referenced in `run.yml`  using {}
    type: file       #type file
    path: name.txt   #variable file path, from which variable values comes from
  ```

  Definitions of eight built-in variables are as followings:
  ```
  $datetime: current datetime
  $date: current date
  $unique: a unique number
  $fullname: a random fake name
  $idcardno: a random fake id card No.
  $cellphone: a random fake cellphone number
  $phonenumber: a random fake phonenumber
  $address: a random fake address
  ```
  
## 3. Run mo-load

**Run Test**

* With the simple below command, all the SQL test cases will automatically run and generate reports and error messages to *report/report.txt* and *report/error.txt*.

```
> ./start.sh
> ./start.sh -c cases/sysbench/point_select_10_100

```

If `start.sh` does not set config path using `-c`, it will just read config files `run.yml` `replace` in current directory.

And you can also specify some parameters when executing the command `./start.sh`, parameters are as followings:

| Parameters |Description|
|------------|---|
| -c         |set config path, mo-load will use run.yml, replace.yml from this path|
| -n         |for sysbench data prepare, set table count, must designate method to SYSBENCH by -m|
| -s         |for sysbench data prepare, set table size, must designate method to SYSBENCH by -m|
| -t         |concurrency that test will run in|
| -m         |method that the test will run with, must be SYSBENCH or None|
| -d         |time that test will last, unit minute|
| -h         |server address|
| -P         |server port|
| -u         |user name for connection|
| -p         |password of user for connection|
| -b         |database for connection|

## 4. Check the report

* Once the test is finished, *mo-load* generates *summary.txt* file, *result.txt* file reports in ./report dir.
* Meanwhile, if there are some errors during test, error messages will be recorded in dir ./report/error dir.

* An example of *summary.txt* file looks like this:

```
[point_select]
START : 2023-04-26 00:15:07
END : 2023-04-26 00:15:53
VUSER : 10
TPS : 528
QPS : 528
SUCCESS : 22771
ERROR : 0
RT_MAX : 175
RT_MIN : 2
RT_AVG : 18.88
RT_25TH : 9.0
RT_75TH : 24.0
RT_90TH : 35.0
RT_99TH : 74.0

```
* An example of *result.txt* file looks like this:

```
|-----------------|-----------------|-----------------|-----------------|-----------------|-----------------|-----------------|
|    TRANSNAME    |      RT_MAX     |      RT_MIN     |      RT_AVG     |       TPS       |      SUCCESS    |      ERROR      |
|-----------------|-----------------|-----------------|-----------------|-----------------|-----------------|-----------------|
[Fri Nov 25 00:09:43 CST 2022]
|    simple_in    |      -1         |      -1         |      null       |       0         |      0          |      0          |
[Fri Nov 25 00:09:44 CST 2022]
|    simple_in    |      186        |      9          |      25.02      |       191       |      189        |      0          |
[Fri Nov 25 00:09:45 CST 2022]
|    simple_in    |      186        |      9          |      26.72      |       184       |      364        |      0          |
[Fri Nov 25 00:09:46 CST 2022]
|    simple_in    |      186        |      9          |      28.06      |       176       |      525        |      0          |
[Fri Nov 25 00:09:47 CST 2022]
|    simple_in    |      223        |      9          |      29.35      |       168       |      673        |      0          |
[Fri Nov 25 00:09:48 CST 2022]
|    simple_in    |      223        |      9          |      30.69      |       161       |      806        |      0          |
[Fri Nov 25 00:09:49 CST 2022]
|    simple_in    |      272        |      9          |      31.74      |       155       |      929        |      0          |
[Fri Nov 25 00:09:50 CST 2022]
```

## 3. How to test sysbench oltp using mo-load

**Fisrt, prepare test data:**
```
> ./start.sh -m SYSBENCH -n 10 -s 10000

```

**Second, define oltp test config:**
This project has definded some common sysbench oltp config in ./cases/sysbench, and these can be used directly.
Also, you can define by yourself according to `How to use MO-Load?`

**Last, run oltp test:**
```
> ./start.sh -c cases/sysbench/point_select_10_100

```

