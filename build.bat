@echo off
set "ORIGINAL_JAVA_HOME=%JAVA_HOME%"
set "JAVA_HOME=D:/Android Studio/jbr"
echo Using JAVA_HOME: %JAVA_HOME%
call gradlew.bat %*
set "JAVA_HOME=%ORIGINAL_JAVA_HOME%"
echo Restored JAVA_HOME: %JAVA_HOME% 