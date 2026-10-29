REM
REM very simple pipeline
REM 1) build w/ unit tests
REM 2) run bootstrap and smoke tests
REM
call build-all.bat %1
call run-bootstrap.bat
