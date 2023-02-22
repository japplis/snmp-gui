#!/bin/sh

PRG=$0
progname=`basename $0`
 
# Resolve symlinks. 
while [ -h "$PRG" ]; do
    ls=`/bin/ls -ld "$PRG"`
    link=`/usr/bin/expr "$ls" : '.*-> \(.*\)$'`
    if /usr/bin/expr "$link" : '/' > /dev/null; then
        PRG="$link"
    else
        PRG="`/usr/bin/dirname $PRG`/$link"
    fi
done
 
ADK_ROOT=`dirname "$PRG"`/..

SNMPPATH=snmpgui.jar:lib/agentapi.jar:lib/java_cup.jar:lib/opennms_joesnmp.jar

java -Dare.home=${ADK_ROOT} -Dare.configdir=${ADK_ROOT}/config -Dapple.laf.useScreenMenuBar=true -cp ${SNMPPATH} com.tryllian.snmp.gui.SnmpGui

