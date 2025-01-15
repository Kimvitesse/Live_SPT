javac -cp "C:\Program Files\Micro-Manager-2.0\plugins\Micro-Manager\MMCoreJ.jar; C:\Program Files\Micro-Manager-2.0\plugins\Micro-Manager\MMAcqEngine.jar; C:\Program Files\Micro-Manager-2.0\plugins\Micro-Manager\MMJ_.jar; C:\Program Files\Micro-Manager-2.0\plugins\Micro-Manager\ij-1.53c.jar" -d ./build -source 8 -target 8 ".\src\fr\telecom\physique\*.java"
cd build
jar cvf LiveSPTPlugin.jar *
cd ..
del "C:\Program Files\Micro-Manager-2.0\mmplugins\LiveSPTPlugin.jar"
copy ".\build\LiveSPTPlugin.jar" "C:\Program Files\Micro-Manager-2.0\mmplugins\"