#!/bin/bash

TESTING_DIRECTORY=/var/lib/jenkins/tempdir
if [ ! -d $TESTING_DIRECTORY ] ; then
    mkdir -p $TESTING_DIRECTORY
fi
TESTING_DIRECTORY_TEMP=$(mktemp -d $TESTING_DIRECTORY/pr-integration.XXXXXX)
ROOT_WORKSPACE=`pwd`
CMO_PIPELINES_DIRECTORY=$ROOT_WORKSPACE/cmo-pipelines
ONCOTREE_DIRECTORY=$ROOT_WORKSPACE/oncotree

ONCOTREE_JAR=$ONCOTREE_DIRECTORY/web/target/oncotree.jar
IMPORT_SCRIPTS_DIRECTORY=$CMO_PIPELINES_DIRECTORY/import-scripts

JENKINS_USER_HOME_DIRECTORY=/var/lib/jenkins
JENKINS_PROPERTIES_DIRECTORY=$JENKINS_USER_HOME_DIRECTORY/pipelines-configuration/properties
APPLICATION_PROPERTIES=application.properties

ONCOTREE_CODE_CONVERTER_TEST_SUCCESS=0
FAKE_ONCOTREE_VERSION_TEST_SUCCESS=0
ONCOTREE_CODE_CONVERTER_OUTPUT_TEST_SUCCESS=0

# will be automatically called when script exits
# provided $ONCOTREE_PORT is defined, will find process number for process on that port and kill it
function find_and_kill_oncotree_process {
    ONCOTREE_PORT_NUMBER=$1
    if [ ! -z $ONCOTREE_PORT_NUMBER ] ; then
        ONCOTREE_PROCESS_NUMBER=`netstat -tanp | grep LISTEN | sed 's/\s\s\s*/\t/g' | grep -P ":$ONCOTREE_PORT_NUMBER\t" | cut -f6 | sed 's/\/.*//'`
    fi
    if [ ! -z $ONCOTREE_PROCESS_NUMBER ] ; then
        kill -9 $ONCOTREE_PROCESS_NUMBER
        if [ $? -gt 0 ] ; then
            echo "failed to kill process $ONCOTREE_PROCESS_NUMBER, please check for running oncotree process"
        fi
    else
            echo "no oncotree process found"
    fi
}
trap 'find_and_kill_oncotree_process $ONCOTREE_PORT' EXIT

function find_free_port {
    CHECKED_PORT=10000
    MAX_PORT=65535
    EXISTING_PORT=PORTNUMBER
    # each loop sets EXISTING_PORT to line from netstat (corresponding to CHECKED_PORT)
    # if grep CHECKED_PORT returns nothing, EXISTING_PORT is unset
    # CHECKED_PORT is a free port since not found in netstat output
    while [[ ! -z $EXISTING_PORT || $CHECKED_PORT -gt $MAX_PORT ]] ; do
        CHECKED_PORT=$(($CHECKED_PORT + 1))
        EXISTING_PORT=`netstat -tanp | sed 's/\s\s\s*/\t/g' | grep -P ":$CHECKED_PORT\t"`
    done
    if [ $CHECKED_PORT -gt $MAX_PORT ] ; then
        echo -1
    else
        echo $CHECKED_PORT
    fi
}

# Copy in ONCOTREE properties and build jar
rsync $JENKINS_PROPERTIES_DIRECTORY/oncotree/$APPLICATION_PROPERTIES $ONCOTREE_DIRECTORY/web/src/main/resources
cd $ONCOTREE_DIRECTORY ; mvn package -Dpackaging.type=jar

#start up ONCOTREE on some port on dashi-dev
ONCOTREE_PORT=`find_free_port`

TIME_BETWEEN_ONCOTREE_AVAILIBILITY_TESTS=3
ONCOTREE_DEPLOYMENT_SUCCESS=0
CURRENT_WAIT_TIME=0
MAXIMUM_WAIT_TIME=600
if [ $ONCOTREE_PORT -gt 0 ] ; then
    java -jar $ONCOTREE_JAR --server.port=$ONCOTREE_PORT &

    # maximum time to wait for oncotree to deploy 2 minutes
    # every 10 seconds check if job is still running
    # attempt to hit endpoint - successful return code indicated ONCOTREE has started up
    ONCOTREE_URL="http://dashi-dev.cbio.mskcc.org:$ONCOTREE_PORT"
    while [ $ONCOTREE_DEPLOYMENT_SUCCESS -eq 0 ] ; do
        CURRENT_WAIT_TIME=$(($CURRENT_WAIT_TIME + 10))
        JOB_RUNNING=`jobs | grep "oncotree.jar" | grep "Running"`
        if [ -z $JOB_RUNNING ] ; then
            curl --fail -X GET --header 'Accept: */*' $ONCOTREE_URL
            if [ $? -eq 0 ] ; then
                ONCOTREE_DEPLOYMENT_SUCCESS=1
                break
            fi
        else
            echo "ONCOTREE unable to start up... canceling tests"
            break
        fi

        if [ $CURRENT_WAIT_TIME -gt $MAXIMUM_WAIT_TIME ] ; then
            echo "ONCOTREE is inaccessible (after a 10 min wait time)... canceling tests"
        fi
        sleep $TIME_BETWEEN_ONCOTREE_AVAILIBILITY_TESTS
    done
fi

if [ $ONCOTREE_DEPLOYMENT_SUCCESS -gt 0 ] ; then
    cp $ONCOTREE_DIRECTORY/integration-test/default_oncotree_file.txt $TESTING_DIRECTORY_TEMP/test_oncotree_default.txt
    cp $ONCOTREE_DIRECTORY/integration-test/default_oncotree_file.txt $TESTING_DIRECTORY_TEMP/test_oncotree_version.txt

    # add headers using test ONCOTREE - failure means new ONCOTREE schema not compatible (i.e invalid endpoint, invalid returned json)
    python $IMPORT_SCRIPTS_DIRECTORY/oncotree_code_converter.py -c $TESTING_DIRECTORY_TEMP/test_oncotree_default.txt -o $ONCOTREE_URL
    if [ $? -gt 0 ] ; then
        echo "call to test ONCOTREE failed -- new ONCOTREE code incompatible with CMO pipelines (oncotree_code_converter.py)"
    else
        ONCOTREE_CODE_CONVERTER_TEST_SUCCESS=1
    fi

    python $IMPORT_SCRIPTS_DIRECTORY/oncotree_code_converter.py -c $TESTING_DIRECTORY_TEMP/test_oncotree_version.txt -o $ONCOTREE_URL -v invalid_oncotree_version
    if [ $? -eq 0 ] ; then
        echo "call to test ONCOTREE with invalid version succeeded -- new ONCOTREE code incompatible with CMO pipelines (oncotree_code_converter.py)"
    else
        FAKE_ONCOTREE_VERSION_TEST_SUCCESS=1
    fi

    python $IMPORT_SCRIPTS_DIRECTORY/oncotree_code_converter.py -c $TESTING_DIRECTORY_TEMP/test_oncotree_version.txt -o $ONCOTREE_URL -v oncotree_candidate_release
    python verify_oncotree_code_converter_output.py -f $TESTING_DIRECTORY_TEMP/test_oncotree_version.txt
    if [ $? -gt 0 ] ; then
        echo "call to test ONCOTREE resulted in unexpected output -- new ONCOTREE code incompatible with CMO pipelines (oncotree_code_converter.py)"
    else
        ONCOTREE_CODE_CONVERTER_OUTPUT_TEST_SUCCESS=1
    fi
fi

rm -rf $TESTING_DIRECTORY_TEMP

# all three tests must pass for integration test to succeed
if [[ $ONCOTREE_CODE_CONVERTER_TEST_SUCCESS -eq 0 || $FAKE_ONCOTREE_VERSION_TEST_SUCCESS -eq 0 || $ONCOTREE_CODE_CONVERTER_OUTPUT_TEST_SUCCESS -eq 0 ]] ; then
    echo "Integration tests for ONCOTREE failed"
    exit 1
fi
