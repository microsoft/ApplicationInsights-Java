set FOLDER=%~dp0
set FILE=%~dp0git-remote-checkout-e2etests.ps1

powershell.exe -ExecutionPolicy Unrestricted -NoProfile -WindowStyle Hidden -File "%FILE%" -RepoUrl https://github.com/microsoft/ApplicationInsights-E2E-Tests.git -BranchName main -RemoteName AppInsights2ETest -RepoFolder $pwd\path\to\checkout
powershell.exe -ExecutionPolicy Unrestricted -NoProfile -WindowStyle Hidden -File "%FILE%" -RepoUrl https://cloudes.visualstudio.com/CDPX/_git/CDPX-Pipeline-Engine -BranchName main -RemoteName AppInsights2ETest -RepoFolder $pwd\path\to\checkout
exit /B %ERRORLEVEL%