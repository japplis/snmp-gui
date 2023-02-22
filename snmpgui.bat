@echo off

rem set ADK_ROOT to the root directory of the ADK installation

if "%ADK_ROOT%"=="" set ADK_ROOT=..
echo ADK_ROOT=%ADK_ROOT%

set SNMPPATH=snmpgui.jar;lib\agentapi.jar;lib\java_cup.jar;lib\opennms_joesnmp.jar

java -Dare.home=%ADK_ROOT% -Dare.configdir=%ADK_ROOT%/config -cp %SNMPPATH% com.tryllian.snmp.gui.SnmpGui
