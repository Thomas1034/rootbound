@echo off
setlocal

:: Define source and destination directories
set "SRC_COMMON=common\build\libs"
set "DST_COMMON=..\modding\1.21.1\verdant\common\libs"

set "SRC_NEOFORGE=neoforge\build\libs"
set "DST_NEOFORGE=..\modding\1.21.1\verdant\neoforge\libs"

set "SRC_FABRIC=fabric\build\libs"
set "DST_FABRIC=..\modding\1.21.1\verdant\fabric\libs"

:: Ensure destination directories exist
mkdir "%DST_COMMON%" 2>nul
mkdir "%DST_NEOFORGE%" 2>nul
mkdir "%DST_FABRIC%" 2>nul

:: Copy files
xcopy /E /Y /V "%SRC_COMMON%\*" "%DST_COMMON%"
xcopy /E /Y /V "%SRC_NEOFORGE%\*" "%DST_NEOFORGE%"
xcopy /E /Y /V "%SRC_FABRIC%\*" "%DST_FABRIC%"

@echo Done!
@echo Press Enter to exit...
pause >nul
endlocal