$ErrorActionPreference = "Stop"
$Separator = "--------------------------------------------------------------------------------------------------------------------------------`r`n"
function Trace-Message
{
    param
    (
        [Parameter(Mandatory=$true, Position=0)]
        [ValidateNotNullOrEmpty()]
        [string]$Message
    )

    $Message = $Message -replace "##vso", "__VSO_DISALLOWED"
    $timestamp = Get-Date
    Write-Host "[INFO] [$timestamp] $Message"
}

Trace-Message "Checking disk space"
Get-WmiObject win32_logicaldisk | Format-Table DeviceId, MediaType, {$_.Size /1GB}, {$_.FreeSpace /1GB}
Write-Host $Separator

Trace-Message "Listing installed 32-bit software"
Get-ItemProperty HKLM:\Software\Wow6432Node\Microsoft\Windows\CurrentVersion\Uninstall\* | Where-Object {$_.DisplayName} | Select-Object DisplayName,DisplayVersion,Publisher,InstallDate | Sort-Object DisplayName,DisplayVersion,Publisher,InstallDate
Write-Host $Separator

Trace-Message "Listing installed 64-bit software"
Get-ItemProperty HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\* | Where-Object {$_.DisplayName} | Select-Object DisplayName,DisplayVersion,Publisher,InstallDate | Sort-Object DisplayName,DisplayVersion,Publisher,InstallDate
Write-Host $Separator
