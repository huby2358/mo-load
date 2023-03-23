#!/usr/bin/env bash

WORKSPACE=$(cd `dirname $0`; pwd)
LIB_WORKSPACE=$WORKSPACE/lib
CONFPATH=.

while getopts ":c:n:m:t:d:s:h:p:u:P:b:H" opt
do
    case $opt in
        c)
        CONFPATH="${OPTARG}"
        ;;
        n)
        expr ${OPTARG} "+" 10 &> /dev/null
        if [ $? -ne 0 ]; then
          echo 'The table count ['${OPTARG}'] is not a number'
          exit 1
        fi
        TABLECOUNT="-n ${OPTARG}"
        ;;
        s)
        expr ${OPTARG} "+" 10 &> /dev/null
        if [ $? -ne 0 ]; then
          echo 'The table size ['${OPTARG}'] is not a number'
          exit 1
        fi
        TABLESIZE="-s ${OPTARG}"
        ;;
        d)
        expr ${OPTARG} "+" 10 &> /dev/null
        if [ $? -ne 0 ]; then
          echo 'The duration ['${OPTARG}'] is not a number'
          exit 1
        fi
        DURATION="-d ${OPTARG}"
        ;;
        t)
        expr ${OPTARG} "+" 10 &> /dev/null
        if [ $? -ne 0 ]; then
          echo 'The threads ['${OPTARG}'] is not a number'
          exit 1
        fi
        THREAD="-t ${OPTARG}"
        ;;
        m)
        METHOD="${OPTARG}"
        ;;
        h)
        SERVER_ADDR="-h ${OPTARG}"
        ;;
        P)
        expr ${OPTARG} "+" 10 &> /dev/null
        if [ $? -ne 0 ]; then
          echo 'The port ['${OPTARG}'] is not a number'
          exit 1
        fi
        
        if [ "${SERVER_ADDR}"x == x ]; then
          SERVER_ADDR="-h 127.0.0.1"
        fi
          
        SERVER_PORT="-P ${OPTARG}"
        ;;
        u)
        USER="-u ${OPTARG}"
        ;;
        p)
        PASSWORD="-p ${OPTARG}"
        ;;
        b)
        DATABASE="--db=${OPTARG}"
        ;;
        H)
        echo -e "Usage:　bash run.sh [option] [param] ...\nExcute mo oltp load task"
        echo -e "   -c  set config path, mo-load will use run.yml, replace.yml from this path"
        echo -e "   -n  for sysbench data prepare, set table count, must designate method to SYSBENCH by -m"
        echo -e "   -s  for sysbench data prepare, set table size, must designate method to SYSBENCH by -m"
        echo -e "   -t  concurrency that test will run in"
        echo -e "   -m  method that the test will run with, must be SYSBENCH or None"
        echo -e "   -d  time that test will last, unit minute"
        echo -e "   -h  server address"
        echo -e "   -P  server port"
        echo -e "   -u  user name for connection"
        echo -e "   -p  password of user for connection"
        echo -e "   -b  database for connection"
        echo "For more support,please email to sudong@matrixorigin.io"
        exit 1
        ;;
        ?)
        echo "Unkown parameter,please use -H to get help."
        exit 1;;
    esac
done


function start {
local libJars libJar
for libJar in `find ${LIB_WORKSPACE} -name "*.jar"`
do
  libJars=${libJars}:${libJar}
done
java -Xms1024M -Xmx30720M -cp ${libJars} \
        -Drun.yml=${CONFPATH}/run.yml \
        -Dreplace.yml=${CONFPATH}/replace.yml \
        io.mo.MOPerfTest ${DURATION} ${THREAD} ${SERVER_ADDR} ${SERVER_PORT} ${USER} ${PASSWORD} ${THREAD} ${DURATION} ${DATABASE}
}

function prepare {
local libJars libJar
for libJar in `find ${LIB_WORKSPACE} -name "*.jar"`
do
  libJars=${libJars}:${libJar}
done
java -Xms1024M -Xmx30720M -cp ${libJars} \
        -Dsysbench.yml=${CONFPATH}/sysbench.yml \
        io.mo.sysbench.Sysbench ${TABLECOUNT} ${TABLESIZE}
}

if [[ "${METHOD}"x != x && "${METHOD}" != "SYSBENCH" ]]; then
  echo "The method must be SYSBENCH or None, [${METHOD}] is not supported"
  exit 1
fi

if [ "${METHOD}" = "SYSBENCH" ]; then
  prepare
else 
  start
fi
