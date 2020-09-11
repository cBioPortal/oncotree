#!/bin/bash

TESTING_DIRECTORY=/var/lib/jenkins/tempdir
if [ ! -d $TESTING_DIRECTORY ] ; then
    mkdir -p $TESTING_DIRECTORY
fi
TESTING_DIRECTORY_TEMP=$(mktemp -d $TESTING_DIRECTORY/pr-integration.XXXXXX)
TESTING_CACHE_DIR="${TESTING_DIRECTORY_TEMP}_ehcache"
TESTING_BACKUP_CACHE_DIR="${TESTING_DIRECTORY_TEMP}_ehcacheback"

ROOT_WORKSPACE=`pwd`
CMO_PIPELINES_DIRECTORY=$ROOT_WORKSPACE/cmo-pipelines
ONCOTREE_DIRECTORY=$ROOT_WORKSPACE/oncotree
ONCOTREE_SCRIPTS_DIRECTORY=$ONCOTREE_DIRECTORY/scripts
ONCOTREE_URI_TO_ONCOTREE_CODE_MAPPING_FILEPATH=$ONCOTREE_DIRECTORY/resources/resource_uri_to_oncocode_mapping.txt

ONCOTREE_JAR=$ONCOTREE_DIRECTORY/web/target/oncotree.jar
IMPORT_SCRIPTS_DIRECTORY=$CMO_PIPELINES_DIRECTORY/import-scripts

JENKINS_USER_HOME_DIRECTORY=/var/lib/jenkins
JENKINS_PROPERTIES_DIRECTORY=$JENKINS_USER_HOME_DIRECTORY/pipelines-configuration/properties
APPLICATION_PROPERTIES=application.properties
TEST_APPLICATION_PROPERTIES=test.application.properties

ONCOTREE_CODE_CONVERTER_TEST_SUCCESS=0
FAKE_ONCOTREE_VERSION_TEST_SUCCESS=0
ONCOTREE_CODE_CONVERTER_OUTPUT_TEST_SUCCESS=0
ONCOTREE_VERSION_MAPPER_TEST_SUCCESS=0
ONCOTREE_TOPBRAID_URI_VALIDATION_SUCCESS=0

TOMCAT_SHUTDOWN_WAIT_TIME=10

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
            echo "failed to kill process $ONCOTREE_PROCESS_NUMBER, please check for running OncoTree process"
        fi
    else
            echo "no OncoTree process found"
    fi
    sleep $TOMCAT_SHUTDOWN_WAIT_TIME
    rm -rf $TESTING_CACHE_DIR
    rm -rf $TESTING_BACKUP_CACHE_DIR
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
rsync $JENKINS_PROPERTIES_DIRECTORY/oncotree/log4j.properties $ONCOTREE_DIRECTORY/web/src/main/resources
rsync $JENKINS_PROPERTIES_DIRECTORY/oncotree/$TEST_APPLICATION_PROPERTIES $ONCOTREE_DIRECTORY/core/src/test/resources/$APPLICATION_PROPERTIES
cd $ONCOTREE_DIRECTORY ; mvn package -Dpackaging.type=jar

#start up ONCOTREE on some port on dashi-dev
ONCOTREE_PORT=`find_free_port`

TIME_BETWEEN_ONCOTREE_AVAILIBILITY_TESTS=20
ONCOTREE_DEPLOYMENT_SUCCESS=0
CURRENT_WAIT_TIME=0
MAXIMUM_WAIT_TIME=600 # 600 seconds (10 min) - as of 3/19/2019 takes 381.809 to start up
if [ $ONCOTREE_PORT -gt 0 ] ; then
    mkdir -p "$TESTING_CACHE_DIR"
    mkdir -p "$TESTING_BACKUP_CACHE_DIR"
    echo "Starting 'java -jar $ONCOTREE_JAR --port=$ONCOTREE_PORT &'"
    java -Dehcache.persistence.path=$TESTING_CACHE_DIR -Dehcache.persistence.backup.path=$TESTING_BACKUP_CACHE_DIR -jar $ONCOTREE_JAR --port=$ONCOTREE_PORT &
    # maximum time to wait for OncoTree to deploy (MAXIMUM_WAIT_TIME/60) minutes
    # every TIME_BETWEEN_ONCOTREE_AVAILIBILITY_TESTS seconds check if job is still running
    # attempt to hit endpoint - successful return code indicated ONCOTREE has started up
    ONCOTREE_URL="http://dashi-dev.cbio.mskcc.org:$ONCOTREE_PORT"
    while [ $ONCOTREE_DEPLOYMENT_SUCCESS -eq 0 ] ; do
        # try sleeping here, checking without a sleep seems to not find the job
        sleep $TIME_BETWEEN_ONCOTREE_AVAILIBILITY_TESTS
        CURRENT_WAIT_TIME=$(($CURRENT_WAIT_TIME + $TIME_BETWEEN_ONCOTREE_AVAILIBILITY_TESTS))
        JOB_RUNNING=`ps -f -u jenkins | grep "oncotree.jar"`
        echo "Status of all jenkins jobs in ps matching oncotree.jar: $JOB_RUNNING"
        # -z is testing if the string is empty, if it is not empty the job is running
        if [ ! -z "$JOB_RUNNING" ] ; then
            curl --fail -X GET --header 'Accept: */*' $ONCOTREE_URL/api/versions
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
            break
        fi
    done
fi

if [ $ONCOTREE_DEPLOYMENT_SUCCESS -gt 0 ] ; then
    INTEGRATION_TEST_DIRECTORY=$ONCOTREE_DIRECTORY/integration-tests
    MOCK_ONCOTREE_FILE=$INTEGRATION_TEST_DIRECTORY/data/default_oncotree_file.txt
    TEST_ONCOTREE_DEFAULT_FILENAME=$TESTING_DIRECTORY_TEMP/test_oncotree_default.txt
    TEST_ONCOTREE_VERSION_FILENAME=$TESTING_DIRECTORY_TEMP/test_oncotree_version.txt
    cp $MOCK_ONCOTREE_FILE $TEST_ONCOTREE_DEFAULT_FILENAME
    cp $MOCK_ONCOTREE_FILE $TEST_ONCOTREE_VERSION_FILENAME

    # add headers using test ONCOTREE - failure means new ONCOTREE schema not compatible (i.e invalid endpoint, invalid returned json)
    python $IMPORT_SCRIPTS_DIRECTORY/oncotree_code_converter.py -c $TEST_ONCOTREE_DEFAULT_FILENAME -o $ONCOTREE_URL
    if [ $? -gt 0 ] ; then
        echo "call to test ONCOTREE failed -- new ONCOTREE code incompatible with CMO pipelines (oncotree_code_converter.py)"
    else
        ONCOTREE_CODE_CONVERTER_TEST_SUCCESS=1
    fi

    python $IMPORT_SCRIPTS_DIRECTORY/oncotree_code_converter.py -c $TEST_ONCOTREE_VERSION_FILENAME -o $ONCOTREE_URL -v invalid_oncotree_version
    if [ $? -eq 0 ] ; then
        echo "call to test ONCOTREE with invalid version succeeded -- new ONCOTREE code incompatible with CMO pipelines (oncotree_code_converter.py)"
    else
        FAKE_ONCOTREE_VERSION_TEST_SUCCESS=1
    fi

    python $IMPORT_SCRIPTS_DIRECTORY/oncotree_code_converter.py -c $TEST_ONCOTREE_VERSION_FILENAME -o $ONCOTREE_URL -v oncotree_candidate_release
    python $INTEGRATION_TEST_DIRECTORY/verify_oncotree_code_converter_output.py $TEST_ONCOTREE_VERSION_FILENAME
    if [ $? -gt 0 ] ; then
        echo "call to test ONCOTREE resulted in unexpected output -- new ONCOTREE code incompatible with CMO pipelines (oncotree_code_converter.py)"
    else
        ONCOTREE_CODE_CONVERTER_OUTPUT_TEST_SUCCESS=1
    fi

    # this test is data dependent (will fail if versions are changed in TopBraid history)
    EXPECTED_ONCOTREE_VERSION_MAPPER_OUTPUT=$INTEGRATION_TEST_DIRECTORY/data/default_oncotree_file_converted.txt
    TEST_ONCOTREE_VERSION_MAPPER_INPUT_FILENAME=$TESTING_DIRECTORY_TEMP/test_oncotree_mapper_version.txt
    TEST_ONCOTREE_VERSION_MAPPER_OUTPUT_FILENAME=$TESTING_DIRECTORY_TEMP/test_oncotree_mapper_version_output.txt
    cp $MOCK_ONCOTREE_FILE $TEST_ONCOTREE_VERSION_MAPPER_INPUT_FILENAME
    python $ONCOTREE_SCRIPTS_DIRECTORY/oncotree_to_oncotree.py -u "$ONCOTREE_URL/api/" -s oncotree_2019_03_01 -t oncotree_2018_05_01 -i $TEST_ONCOTREE_VERSION_MAPPER_INPUT_FILENAME -o $TEST_ONCOTREE_VERSION_MAPPER_OUTPUT_FILENAME
    diff $EXPECTED_ONCOTREE_VERSION_MAPPER_OUTPUT $TEST_ONCOTREE_VERSION_MAPPER_OUTPUT_FILENAME
    if [ $? -gt 0 ] ; then
        echo "oncotree_to_oncotree.py output differs from expected output"
    else
        ONCOTREE_VERSION_MAPPER_TEST_SUCCESS=1
    fi
fi

rm -rf $TESTING_DIRECTORY_TEMP

# test that the resource_uri_to_oncocode_mapping.txt is valid and matches TopBraid
python $ONCOTREE_SCRIPTS_DIRECTORY/validate_topbraid_uris.py --curated-file $ONCOTREE_URI_TO_ONCOTREE_CODE_MAPPING_FILEPATH --properties-file $JENKINS_PROPERTIES_DIRECTORY/oncotree/$APPLICATION_PROPERTIES
if [ $? -gt 0 ] ; then
    echo "validate_topbraid_uris.py failed, resource_uri_to_oncocode_mapping.txt is invalid or in conflict with TopBraid"
else
    ONCOTREE_TOPBRAID_URI_VALIDATION_SUCCESS=1
fi

# all five tests must pass for integration test to succeed
if [[ $ONCOTREE_CODE_CONVERTER_TEST_SUCCESS -eq 0 || $FAKE_ONCOTREE_VERSION_TEST_SUCCESS -eq 0 || $ONCOTREE_CODE_CONVERTER_OUTPUT_TEST_SUCCESS -eq 0 || $ONCOTREE_VERSION_MAPPER_TEST_SUCCESS -eq 0 || $ONCOTREE_TOPBRAID_URI_VALIDATION_SUCCESS -eq 0 ]] ; then
    echo "Integration tests for ONCOTREE failed"
    exit 1
fi
