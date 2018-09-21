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
DB_OPTS="-Dspring.config=$APPDIR/../properties/default_db.xml"
LOG4J_OPTS="-Dlog4j.configuration=file://$APPDIR/properties/log4j.properties"
export HUMAN_PROTEOME_MAP_OPTS="$DB_OPTS $LOG4J_OPTS"

bin/$APPNAME "$@" 2>&1 > $APPDIR/run.log

mailx -s "[$SERVER] Human Proteome Map pipeline run" $EMAIL_LIST < $APPDIR/logs/status.log

