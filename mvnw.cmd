@echo off
setlocal
set PRG=%~dp0mvnw.cmd
set BASEDIR=%~dp0
if "%BASEDIR:~-1%"=="\" set BASEDIR=%BASEDIR:~0,-1%
set MAVEN_WRAPPER_JAR=%BASEDIR%\.mvn\wrapper\maven-wrapper.jar
if defined JAVA_HOME (
  set JAVACMD=%JAVA_HOME%\bin\java.exe
) else (
  set JAVACMD=java
)
"%JAVACMD%" -classpath "%MAVEN_WRAPPER_JAR%" -Dmaven.multiModuleProjectDirectory="%BASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*
endlocal
