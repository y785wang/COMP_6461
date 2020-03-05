@echo off
echo Test Scenario: Several clients reading the same file

set /p nClient=Number of clients: 
set /p file=File name to get: 
for /l %%i in (1, 1, %nClient%) do (
  start cmd.exe @cmd /k "java com.client.httpc get http://localhost/%file% -v"
)
