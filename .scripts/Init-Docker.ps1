$ErrorActionPreference = "Stop"
Import-Module "$PSScriptRoot\win-installer-helper.psm1" -DisableNameChecking

$dockerPath = (cmd /c "where docker.exe")
if ($?)
{
     Add-EnvironmentVariable -Name "DOCKER_EXE" -Value $dockerPath
}
else
{
    Write-Error -Message "Could not find docker.exe"
}
