$url = "https://repos.azul.com/azure-only/zulu/packages/zulu-11/11.0.7/zulu-11-azure-jdk_11.39.15-11.0.7-win_x64.zip"
$fileName = "zulu-11-azure-jdk_11.39.15-11.0.7-win_x64.zip"
$source = "C:\Downloads\$fileName"
$destination = "C:\Program Files\Java\"
$envVarKey = "JAVA_HOME"
$envVarValue = "C:\Program Files\Java\zulu-11-azure-jdk_11.39.15-11.0.7-win_x64"
$pathUpdate = "C:\Program Files\Java\zulu-11-azure-jdk_11.39.15-11.0.7-win_x64\bin"

$ErrorActionPreference = "Stop"

if (Test-Path "$PSScriptRoot\win-installer-helper.psm1")
{
    Import-Module "$PSScriptRoot\win-installer-helper.psm1" -DisableNameChecking
} elseif (Test-Path "$PSScriptRoot\..\..\Helpers\win-installer-helper.psm1")
{
    Import-Module "$PSScriptRoot\..\..\Helpers\win-installer-helper.psm1" -DisableNameChecking
}

Start-Setup
$PathNodes=@()
try
{
    Get-File -Url $url -FileName $fileName
    Expand-ArchiveWith7Zip -Source $source -Destination $destination
    Add-EnvironmentVariable -Name $envVarKey -Value $envVarValue
    $PathNodes += $pathUpdate
}
finally
{
    if (!$PathNodes -eq "")
    {
      Update-Path -PathNodes $PathNodes
    }
    Stop-Setup
}
