@echo off
echo Test Scenario: Several clients writing to the same file

set /p nClient=Number of clients: 
set /p file=File name to post: 
for /l %%i in (1, 1, %nClient%) do (
  start cmd.exe @cmd /k "java com.client.httpc post http://localhost/%file% -f %file% -v"
)
