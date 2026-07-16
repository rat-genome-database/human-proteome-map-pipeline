#!/usr/bin/env bash
# shell script to run HumanProteomeMap pipeline
. /etc/profile

APPNAME="human-proteome-map-pipeline"
APPDIR=/home/rgddata/pipelines/$APPNAME
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`

EMAIL_LIST=mtutaj@mcw.edu
if [ "$SERVER" == "REED" ]; then
  EMAIL_LIST=mtutaj@mcw.edu
fi

cd $APPDIR
java -Dspring.config=$APPDIR/../properties/default_db2.xml \
    -Dlog4j.configurationFile=file://$APPDIR/properties/log4j2.xml \
    -jar lib/${APPNAME}.jar "$@" > $APPDIR/run.log 2>&1
EXIT_CODE=$?

# on failure summary.log could be empty or stale, so mail run.log: it has the stack trace
if [ $EXIT_CODE -ne 0 ]; then
  mailx -s "[$SERVER] Human Proteome Map pipeline FAILED (exit code $EXIT_CODE)" $EMAIL_LIST < $APPDIR/run.log
  exit $EXIT_CODE
fi

mailx -s "[$SERVER] Human Proteome Map pipeline run" $EMAIL_LIST < $APPDIR/logs/summary.log

