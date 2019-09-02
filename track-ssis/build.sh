#!/bin/bash

function CheckCmd(){
    if [ $? != 0 ]; then
        echo "$1"
        exit -1
    fi
}

BRANCH=$1

if [ -z ${BRANCH} ]; then
    BRANCH=master
fi

git checkout -f ${BRANCH}
CheckCmd

git submodule update --init --recursive

git submodule foreach git checkout -f master
git submodule foreach git pull
git pull

JAR_NAME=ssis-serve
### create

mvn clean install
CheckCmd

JARS=/data/download/jars/${BRANCH}
mkdir -p ${JARS}
CheckCmd

mv ${JAR_NAME}/target/${JAR_NAME}-1.0-SNAPSHOT.jar ${JARS}/${JAR_NAME}.jar
CheckCmd

echo "the jar at $JARS directory"

