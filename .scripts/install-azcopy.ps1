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

    if ($SkipUnzip)
    {
        Write-Host "Skipping Unzip"
        Write-Host "Destination $Destination"
        if($Destination.ToString() -eq "C:\7-Zip") {
            Write-Host "Source $Source"
            Start-Process -Wait -FilePath "$Source" -ArgumentList "/S"
            if (-not (Test-Path "C:\7-Zip\7z.exe")) {
                Write-Error "7-Zip wasn't installed successfully and C:\7-Zip\7z.exe does not exist."
            } else {
                Write-Host "C:\7-Zip\7z.exe exists now.";
            }
        } else {
            Write-Error "Can't install 7-Zip somehow..."
        }
    }
    else
    {
        Write-Host "Unzipping $Source to $Destination"
        Expand-Archive -LiteralPath $Source -Destination $Destination -Force
        Write-Host "Finished unzipping to $Destination"

        #$process = Start-Process $Path -PassThru -Verbose -NoNewWindow -ArgumentList "x -aoa -y `"$Source`" -o`"$Destination`""
        #$thePid = $process.Id
        #$pn = [System.IO.Path]::GetFileNameWithoutExtension($Path)

        #Trace-Message "Started EXE asynchronously. Process ID is $thePid"

        #Wait-ForProcess -Process $process -Minutes $WaitMinutes
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
