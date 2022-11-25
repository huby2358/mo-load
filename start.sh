#!/usr/bin/env bash

WORKSPACE=$(cd `dirname $0`; pwd)
LIB_WORKSPACE=$WORKSPACE/lib
CONFPATH=.
DURATION=0
THREAD=0

while getopts ":c:n:m:t:d:s:h" opt
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
        TABLECOUNT="${OPTARG}"
        ;;
        s)
        expr ${OPTARG} "+" 10 &> /dev/null
        if [ $? -ne 0 ]; then
          echo 'The table size ['${OPTARG}'] is not a number'
          exit 1
        fi
        TABLESIZE="${OPTARG}"
        ;;
        d)
        expr ${OPTARG} "+" 10 &> /dev/null
        if [ $? -ne 0 ]; then
          echo 'The duration ['${OPTARG}'] is not a number'
          exit 1
        fi
        DURATION="${OPTARG}"
        ;;
        t)
        expr ${OPTARG} "+" 10 &> /dev/null
        if [ $? -ne 0 ]; then
          echo 'The threads ['${OPTARG}'] is not a number'
          exit 1
        fi
        THREAD="${OPTARG}"
        ;;
        m)
        METHOD="${OPTARG}"
        ;;
        h)
        echo -e "Usage:　bash run.sh [option] [param] ...\nExcute mo oltp load task"
        echo -e "   -c  set config path, mo-load will use run.yml, replace.yml from this path"
        echo -e "   -n  for sysbench data prepare, set table count, must designate method to SYSBENCH by -m"
        echo -e "   -s  for sysbench data prepare, set table size, must designate method to SYSBENCH by -m"
        echo -e "   -t  concurrency that test will run in"
        echo -e "   -m  method that the test will run with, must be SYSBENCH or None"
        echo -e "   -d  time that test will last, unit minute"
        echo "For more support,please email to sudong@matrixorigin.io"
        exit 1
        ;;
        ?)
        echo "Unkown parameter,please use -h to get help."
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
        io.mo.MOPerfTest ${DURATION} ${THREAD}
}

function prepare {
local libJars libJar
for libJar in `find ${LIB_WORKSPACE} -name "*.jar"`
do
  libJars=${libJars}:${libJar}
done
java -Xms1024M -Xmx30720M -cp ${libJars} \
        -Dsysbench.yml=${CONFPATH}/sysbench.yml \
        io.mo.Sysbench ${TABLECOUNT} ${TABLESIZE}
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
