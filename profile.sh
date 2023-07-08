#!/usr/bin/env bash
WORKSPACE=$(cd `dirname $0`; pwd)
pip3 install shyaml

function profile() {
    if [ -f mo.yml ]; then
      local addr=`cat mo.yml | shyaml get-value profile.addr`
      if [ "${addr}"x == x ]; then
        echo "`date +'%Y-%m-%d %H:%M:%S'` The profile server addr is not configured, please check."
        exit 0;
      fi 
      
      local port=`cat mo.yml | shyaml get-value profile.port`
      if [ "${port}"x == x ]; then
        echo "`date +'%Y-%m-%d %H:%M:%S'` The profile server port is not configured, please check."
        exit
      fi
            
      local think=`cat mo.yml | shyaml get-value profile.think`
      if [ "${port}"x == x ]; then
        think=60
      fi
      
      echo "`date +'%Y-%m-%d %H:%M:%S'` The profile server addr is ${addr}"
      echo "`date +'%Y-%m-%d %H:%M:%S'` The profile server port is ${port}"
    else
      exit 0;
    fi
    
    echo "`date +'%Y-%m-%d %H:%M:%S'` Start to sleep for ${think} second, please wait...."
    sleep ${think}
    
    if [ -f ${WORKSPACE}/report/.run ]; then
      local runid=`cat ${WORKSPACE}/report/.run`
      OLD_IFS="$IFS"
      IFS=","
      addrs=(${addr})
      IFS="$OLD_IFS"
      
      for ad in ${addrs[@]}
      do
        echo "`date +'%Y-%m-%d %H:%M:%S'` Start to get profile from server ${ad}" 
        mkdir -p ${WORKSPACE}/report/${runid}/prof/${ad}
        curl http://${ad}:${port}/debug/pprof/profile?seconds=30 > ${WORKSPACE}/report/${runid}/prof/${ad}/cpu
        curl http://${ad}:${port}/debug/pprof/heap > ${WORKSPACE}/report/${runid}/prof/${ad}/heap
        curl http://${ad}:${port}/debug/pprof/goroutine?debug=2 -o ${WORKSPACE}/report/${runid}/prof/${ad}/goroutine.log
#        curl http://${ad}:${port}/debug/pprof/trace?seconds=10 -o ${WORKSPACE}/report/${runid}/prof/${ad}/trace.out
        echo "`date +'%Y-%m-%d %H:%M:%S'` Finish to get profile from server ${ad}"
      done 
    else
      exit 0;
    fi 
}

profile