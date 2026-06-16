@echo off
rem Elements of Earth (EoE) ランチャー — ダブルクリックで起動します。
cd /d "%~dp0"
call gradlew.bat run
pause
