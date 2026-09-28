@echo off
setlocal EnableExtensions EnableDelayedExpansion
REM Scores the bundled JPEG2000 sample (src\test\resources\info_jp2.iso)
REM with the NfiqApplication harness. Lives in nfiq1.0; can be called from any folder.
REM Linux / macOS / Git Bash: use run-local-JP2.sh
REM
REM   run-local-JP2.bat [0|1]        0 = one-line score (default), 1 = detailed quality map
REM   run-local-JP2.bat build [0|1]  force a fresh "mvn clean package" first
REM Optional env: JAVA_OPTS (extra JVM flags)

set "IMG_NAME=info_jp2.iso"

set "MODULE_DIR=%~dp0"
if "%MODULE_DIR:~-1%"=="\" set "MODULE_DIR=%MODULE_DIR:~0,-1%"
set "IMG_REL=src\test\resources\%IMG_NAME%"
set "LOG_DIR=%MODULE_DIR%\.local\logs"
set "MAIN_CLASS=org.mosip.nist.nfiq1.test.NfiqApplication"

set "FORCE_BUILD="
if /I "%~1"=="build" (
  set "FORCE_BUILD=1"
  shift
)
set "LOGS=%~1"
if "%LOGS%"=="" set "LOGS=0"

if not exist "%MODULE_DIR%\%IMG_REL%" (
  echo error: sample not found: %MODULE_DIR%\%IMG_REL%
  exit /b 1
)
where java >nul 2>&1
if errorlevel 1 (
  echo error: java ^(JDK 21^) is required on PATH
  exit /b 1
)

call :find_jar
if defined FORCE_BUILD set "LIB_JAR="
if not defined LIB_JAR (
  call :build
  if errorlevel 1 exit /b 1
  call :find_jar
  if not defined LIB_JAR (
    echo error: build produced no nfiq1.0 jar in target
    exit /b 1
  )
)

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
set "RUN_LOG=%LOG_DIR%\nfiq-%IMG_NAME:.iso=%.log"

echo ==^> NFIQ 1.0  imgfile=%IMG_REL%  logs=%LOGS%
REM ImageDecoder resolves imgfile relative to the working directory, so run from nfiq1.0.
REM JVM options (-D..., --enable-preview) must precede -cp / main class.
pushd "%MODULE_DIR%"
java %JAVA_OPTS% --enable-preview -Dfile.encoding=UTF-8 -cp "%LIB_JAR%;%MODULE_DIR%\target\lib\*;%MODULE_DIR%\target\test-classes" %MAIN_CLASS% "imgfile=%IMG_REL%" "logs=%LOGS%" > "%RUN_LOG%" 2>&1
set "RC=%ERRORLEVEL%"
popd
type "%RUN_LOG%"
echo     log %RUN_LOG%
if not "%RC%"=="0" echo error: NfiqApplication exited with %RC%
exit /b %RC%

:find_jar
set "LIB_JAR="
if not exist "%MODULE_DIR%\target\test-classes\org\mosip\nist\nfiq1\test\NfiqApplication.class" exit /b 0
for %%F in ("%MODULE_DIR%\target\nfiq1.0-*.jar") do (
  echo %%~nxF | findstr /I /C:"sources" /C:"javadoc" >nul
  if errorlevel 1 (
    set "LIB_JAR=%%~fF"
    exit /b 0
  )
)
exit /b 0

:build
where mvn >nul 2>&1
if errorlevel 1 (
  echo error: mvn is required on PATH to build the jar
  exit /b 1
)
echo ==^> building nfiq1.0 ^(skip tests^)
pushd "%MODULE_DIR%"
call mvn -q clean package -DskipTests "-Dgpg.skip=true" "-Dmaven.javadoc.skip=true"
set "BRC=%ERRORLEVEL%"
popd
exit /b %BRC%
