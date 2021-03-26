param (
    [Parameter(Mandatory=$true, HelpMessage="Url to download. Must point to a zip file. Assumes filename is last path element. Assumes zip contains top level directory with same name as zip file.")]
    [ValidateNotNullOrEmpty()]
    [string]$Url,

    [Parameter(Mandatory=$true, HelpMessage="Destination directory to unzip downloaded file.")]
    [ValidateNotNullOrEmpty()]
    [System.IO.FileInfo]$Destination,

    [Parameter(HelpMessage="When used, adds bin directory to PATH.")]
    [switch]$UpdatePath = $false,

    [Parameter(HelpMessage="When used, downloads directory will be cleaned and temp directories cleaned.")]
    [switch]$CleanOnFinish = $false,

    [Parameter(Mandatory=$false, HelpMessage="7Zip location. Should point to 7z.exe.")]
    [ValidateNotNullOrEmpty()]
    [System.IO.FileInfo]$PathTo7Zip,

    [Parameter(Mandatory=$false, HelpMessage="Skips download step.")]
    [switch]$SkipDownload = $false,

    [Parameter(Mandatory=$false, HelpMessage="Skips unzip.")]
    [switch]$SkipUnzip = $false
)

$fileName, $dirName = ($Url | Select-String -Pattern ".+/(?<filename>(?<dirname>[^/]+)\..+)$").Matches[0].Groups['filename', 'dirname'].Value
$Source = [System.IO.Path]::Combine("C:\Downloads", $fileName)

$ErrorActionPreference = "Stop"
Import-Module "$PSScriptRoot\win-installer-helper.psm1" -DisableNameChecking

Start-Setup

$PathNodes=@()
try
{
    if (-not $SkipDownload) {
        Write-Host "Downloading '$fileName' from '$Url' to '$Source'"
        Get-File -Url $url -FileName $fileName
        Write-Host "Download finished: $Source"
    }
    else
    {
        Write-Host "Skipping download."
        if (-not (Test-Path $Source)) {
            Write-Error "$Source does not exist"
            exit
        }
    }

    if (-not $SkipUnzip)
    {
         Write-Host "Unzipping $Source to $Destination"
         Expand-Archive -LiteralPath $Source -Destination $Destination -Force
         Write-Host "Finished unzipping to $Destination"
    }
}
finally
{
    if (!$PathNodes -eq "")
    {
        Write-Host "Appending to PATH: '$PathNodes'"
        Update-Path -PathNodes $PathNodes
    }
    if ($CleanOnFinish)
    {
        Stop-Setup
    }
    else
    {
        Stop-Setup -PreserveTemp -PreserveDownloads
    }
}
