@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Startup Batch Script for Windows
@REM ----------------------------------------------------------------------------
@IF "%DEBUG%" == "" @ECHO OFF
@SETLOCAL EnableExtensions EnableDelayedExpansion

SET "DIRNAME=%~dp0"
IF "%DIRNAME%" == "" SET "DIRNAME=."
SET "APP_BASE_NAME=%~n0"
SET "APP_HOME=%DIRNAME%"

@REM Execute Maven Wrapper
SET MAVEN_JAVA_EXE="%JAVA_HOME%\bin\java.exe"
IF NOT EXIST %MAVEN_JAVA_EXE% SET MAVEN_JAVA_EXE=java.exe

SET WRAPPER_JAR="%APP_HOME%\.mvn\wrapper\maven-wrapper.jar"
SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

IF EXIST %WRAPPER_JAR% (
    %MAVEN_JAVA_EXE% -Dmaven.multiModuleProjectDirectory="%APP_HOME%" -cp %WRAPPER_JAR% %WRAPPER_LAUNCHER% %*
) ELSE (
    mvn %*
)
