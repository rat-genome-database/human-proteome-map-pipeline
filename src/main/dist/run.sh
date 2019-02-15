#!/usr/bin/env bash
# shell script to run HumanProteomeMap pipeline
. /etc/profile

APPNAME=HumanProteomeMap
APPDIR=/home/rgddata/pipelines/$APPNAME
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`

EMAIL_LIST=mtutaj@mcw.edu
if [ "$SERVER" == "REED" ]; then
  EMAIL_LIST=rgd.developers@mcw.edu
fi

cd $APPDIR
java -Dspring.config=$APPDIR/../properties/default_db2.xml \
    -Dlog4j.configuration=file://$APPDIR/properties/log4j.properties \
    -jar lib/${APPNAME}.jar "$@" 2>&1 > $APPDIR/run.log

mailx -s "[$SERVER] Human Proteome Map pipeline run" $EMAIL_LIST < $APPDIR/logs/summary.log

