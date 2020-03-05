@echo off
echo Test Scenario: One client writing and one client reading the same file

set /p file=File name to post/get: 
start cmd.exe @cmd /k "java com.client.httpc get http://localhost/%file% -v"
start cmd.exe @cmd /k "java com.client.httpc post http://localhost/%file% -f %file% -v"
