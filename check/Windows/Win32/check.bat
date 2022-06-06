@echo off
set total=0
set success=0
set error=0
if exist log.txt (
  del log.txt
)
for /R %cd% %%s in (*) do (
  echo %%~ns%%~xs | findstr .tar >nul && (
    set /a total+=1
    IF %%~xs==.tar (
      tar -tf %%s >nul 2>nul && (set /a success+=1 & echo Success) || (set /a error+=1 & echo Error & echo %%s >> log.txt)
    )
    IF %%~xs==.zst (
      zstd.exe -t %%s >nul 2>nul && (set /a success+=1 & echo Success) || (set /a error+=1 & echo Error & echo %%s >> log.txt)
    )
    IF %%~xs==.lz4 (
      zstd.exe -t %%s >nul 2>nul && (set /a success+=1 & echo Success) || (set /a error+=1 & echo Error & echo %%s >> log.txt)
    )
  )
)
echo Total:%total%
echo Success:%success%
echo Error:%error%
if exist log.txt (
  echo Broken files:
  type log.txt
  del log.txt
)
pause
