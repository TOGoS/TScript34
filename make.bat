setlocal
rem Here so you can override, in case your local JDK
rem refuses to build anything in such an old fashion:
if not defined jvm_source_version set jvm_source_version=1.6
if not defined jvm_target_version set jvm_target_version=1.6

if exist target\classes goto find_sources
mkdir target\classes
if errorlevel 1 exit /B 1

:find_sources
cd src\main\java && dir *.java /b/s >..\..\..\.java-sources.lst && cd ..\..\..
if errorlevel 1 exit /B 1

javac -d target\classes -source %jvm_source_version% -target %jvm_target_version% @.java-sources.lst
if errorlevel 1 exit /B 1

jar --create --file=target/JCR36-dev.jar --main-class=net.nuke24.jcr36.SimpleCommandRunner -C target\classes .
if errorlevel 1 exit /B 1
