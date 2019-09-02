#!/bin/bash

ARTIFACT_URL=${JAR_URL}
ARTIFACT_ENV=${JAR_ENV}
ARTIFACT_NAME=${JAR_NAME}

ARTIFACT=${ARTIFACT_NAME}
PROJECT=/mnt/${ARTIFACT}

### setting DNS
RESOLVE_CONF=/etc/resolv.conf
if [[ ! -f "$RESOLVE_CONF.bak" ]] ; then
    cp ${RESOLVE_CONF} ${RESOLVE_CONF}.bak
fi
cp ${RESOLVE_CONF}.bak ${RESOLVE_CONF}
echo "nameserver 114.114.114.114" >> ${RESOLVE_CONF}
echo "nameserver 223.5.5.5" >> ${RESOLVE_CONF}

### HOST_mapping=ip__domain
if [[ ! -z ${HOST_mapping} ]]; then
    HOST_mapping=${HOST_mapping//__/ }
    echo ${HOST_mapping} >> /etc/hosts
fi

sleep 1;

if [[ -z ${ARTIFACT_NAME} ]] || [[ -z ${ARTIFACT_ENV} ]] ; then
    echo "must require 2 params: JAR_NAME, JAR_ENV"
    exit -1
fi

if [[ -z ${ARTIFACT_URL} ]] ; then
    echo "please the latest $JAR_NAME jar already in $PROJECT otherwise give me a download JAR_URL"
fi

function CheckCmd(){
    if [[ $? != 0 ]] ; then
        echo "$1"
        exit -1
    fi
}

mkdir -p ${PROJECT}
CheckCmd
cd ${PROJECT}
if [[ ! -z ${ARTIFACT_URL} ]] ; then
    if [[ -f "/usr/local/bin/container.env" ]] ; then
        JAR_download=`cat /usr/local/bin/container.env|awk '{printf $0}'`
    fi
    if [[ -z ${JAR_download} ]]; then
        if [[ -f "$ARTIFACT.jar" ]] ; then
            rm ${ARTIFACT}.jar
            CheckCmd
        fi
        wget ${ARTIFACT_URL}
        CheckCmd
    elif [[ ! -f "$ARTIFACT.jar" ]] ; then
        wget ${ARTIFACT_URL}
        CheckCmd
    fi
    echo may_wget=0 > /usr/local/bin/container.env
fi

if [[ ! -f "$ARTIFACT.jar" ]];then
    mv *.jar ${ARTIFACT}.jar
    CheckCmd
fi

### jvm optimization args，
#jvm 的最小 heap 大小，建议和-Xmx一样， 防止因为内存收缩／突然增大带来的性能影响。默认值1024M
if [[ -z ${JVM_Xms} ]]; then
    JVM_Xms=1024M
fi
#jvm 的最大 heap 大小。默认值1024M
if [[ -z ${JVM_Xmx} ]]; then
    JVM_Xmx=1024M
fi
#jvm 中 New Generation 的大小，这个参数很影响性能，如果你的程序需要比较多的临时内存，建议设置到512M，如果用的少，尽量降低这个数值，一般来说128／256足以使用了。默认值512M
if [[ -z ${JVM_Xmn} ]]; then
    JVM_Xmn=512M
fi
#Direct Memory使用到达了这个大小，就会强制触发Full GC。默认值1024M
if [[ -z ${JVM_MaxDirectMemorySize} ]]; then
    JVM_MaxDirectMemorySize=1024M
fi
#年老代和新生代的堆内存占用比例，即-XX:NewRatio=老年代/新生代，不允许-XX:Newratio值小于1，默认值 1
if [[ -z ${JVM_NewRatio} ]]; then
    JVM_NewRatio=1
fi
nohup java -server -Xms${JVM_Xms} -Xmx${JVM_Xmx} -Xmn${JVM_Xmn} -XX:MaxDirectMemorySize=${JVM_MaxDirectMemorySize} -XX:NewRatio=${JVM_NewRatio} -XX:+UseNUMA -XX:+UseParallelGC \
           -jar ${ARTIFACT}.jar --spring.profiles.active=${ARTIFACT_ENV} > nohup.out 2>&1 &
