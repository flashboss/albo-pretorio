#!/bin/bash
# Alfresco export script
# Copy or symlink in your $ALF_HOME/bin.
 
#if [ ! -f "$JAVA_HOME/bin/java" ]
#then
#    echo "JAVA_HOME not set or incorrect."
#    exit
#fi
if [ "$ALF_HOME" == "" ]
then
    ALF_HOME="`dirname $0`/.."
fi
APPSERVER="${ALF_HOME}/tomcat"
ALF_WEBINF="$APPSERVER/webapps/alfresco/WEB-INF"
if [ ! -d "$ALF_WEBINF" ]
then
    echo "ALF_HOME not set or incorrect."
    exit
fi
 
JAVA_OPTS="-Xms128m -Xmx1024m -XX:MaxMetaspaceSize=256m -server"
 
CLASSPATH=$ALF_WEBINF/classes/alfresco/module:$APPSERVER/shared/classes:$ALF_WEBINF/classes
 
# List all libs
for f in `find $ALF_WEBINF/lib` $APPSERVER/lib/ojdbc* $APPSERVER/lib/servlet* $APPSERVER/lib/mysql* $APPSERVER/lib/postgre*
do
    CLASSPATH=$CLASSPATH:$f
done
ALF_OPTS="-Dvti.server.port=0 -Dcifs.enabled=false -Dftp.enabled=false -Dnfs.enabled=false -Demail.server.enabled=false -Dldap.synchronization.active=false -Dimap.server.enabled=false -Daudit.enabled=false -Dtransferservice.receiver.enabled=false -Dalfresco.rmi.services.port=0 -Dooo.enabled=false -Dooo.exe= -Djodconverter.enabled=false"
java $JAVA_OPTS $ALF_OPTS -classpath $CLASSPATH org.alfresco.tools.Export "$@" 
