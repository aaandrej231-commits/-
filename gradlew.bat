@ECHO OFF
SETLOCAL
WHERE gradle >NUL 2>NUL
IF %ERRORLEVEL% EQU 0 (
  gradle %*
  EXIT /B %ERRORLEVEL%
)
ECHO Gradle is not installed. Install Gradle 8.8 or run this project from a Unix-like environment with curl and unzip.
EXIT /B 1
