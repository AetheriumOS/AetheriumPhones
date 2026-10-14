#!/usr/bin/env sh

APP_BASE_NAME=`basename "$0"`
APP_HOME=`cd "$(dirname "$0")"; pwd`

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Java home
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

exec "$JAVACMD" -jar "$CLASSPATH" "$@"
