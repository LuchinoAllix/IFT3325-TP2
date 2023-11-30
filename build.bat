ECHO compiling
javac -d .\bin -source 16 -target 16  .\src\*.java
ECHO building Sender
jar cfm .\build\sender.jar .\sender_manifest.txt -C bin\ .
ECHO building receiver
jar cfm .\build\receiver.jar .\receiver_manifest.txt -C bin\ .
ECHO terminé