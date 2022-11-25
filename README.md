
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
  
  #It is valid only when prepared=true. 
  #The format is INT (value), STR (value). Value can be a variable or constant. 
  #The count of parameters must be the same as the count of parameters in the preparedstatement statement
  paras: INT({sequence}),STR({string})
  
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

## 4. Check the report

* Once the test is finished, *mo-load* generates *summary.txt* file, *result.txt* file reports in ./report dir.
* Meanwhile, if there are some errors during test, error messages will be recorded in dir ./report/error dir.

* An example of *summary.txt* file looks like this:

```
[simple_in]
RT_MAX : 566
RT_MIN : 9
RT_AVG : 59.79
TPS : 83
SUCCESS : 5023
ERROR : 0

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
