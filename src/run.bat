@ECHO OFF

REM #####
REM # PLEASE UPDATE run.sh when you change this file (at least add a "# FIXME")
REM #####

:top
java -cp ^
.;^
lib/c3p0-0.9.1.2.jar;^
lib/msnm.jar;^
lib/jcfd.jar;^
lib/jazzy-core.jar;^
lib/bsh-2.0b4.jar;^
mysql-connector-java-5.1.5-bin.jar;^
lib/pircbot.jar;^
lib/js-rhino-1.6r2.jar;^
lib/jersey.jar;^
lib/jsr311-api.jar;^
lib/asm-3.1.jar;^
 ^
uk.co.uwcs.choob.ChoobMain
IF "%ERRORLEVEL%"=="0" GOTO :EOF
IF "%1"=="once" GOTO :EOF
SLEEP 15
GOTO :top
