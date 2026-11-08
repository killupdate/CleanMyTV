@echo off
echo CleanMyTV: external executable downloads are disabled. Owner approval and dependency audit are required; see README.md. 1>&2
exit /b 1
@echo off
rem Reuse the repository wrapper; this add-on remains its own Gradle project.
call "%~dp0..\..\launcher\gradlew.bat" -p "%~dp0." %*
