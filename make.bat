if exist target\classes goto find_sources
mkdir target\classes
if errorlevel 1 exit /B 1

:find_sources
cd src\main\java && dir *.java /b/s >..\..\..\.java-sources.lst && cd ..\..\..
if errorlevel 1 exit /B 1

javac -d target\classes -source 1.6 -target 1.6 @.java-sources.lst
if errorlevel 1 exit /B 1

jar --create --file=target/JCR36.0.3.jar --main-class=net.nuke24.jcr36.SimpleCommandRunner -C target\classes .
if errorlevel 1 exit /B 1
