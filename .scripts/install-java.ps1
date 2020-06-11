param (
    [Parameter(Mandatory=$true, HelpMessage="Url to download. Must point to a zip file. Assumes filename is last path element. Assumes zip contains top level directory with same name as zip file.")]
    [ValidateNotNullOrEmpty()]
    [string]$Url,

    [Parameter(Mandatory=$true, HelpMessage="Destination directory to unzip downloaded file.")]
    [ValidateNotNullOrEmpty()]
    [System.IO.FileInfo]$Destination,

    [Parameter(Mandatory=$true, HelpMessage="Environment variable to store destination path, e.g. JAVA_HOME")]
    [ValidateNotNullOrEmpty()]
    [string]$EnvVar,

    [Parameter(HelpMessage="When used, adds bin directory to PATH.")]
    [switch]$UpdatePath = $false,

    [Parameter(Mandatory=$true, HelpMessage="Checksum to use to validate download.")]
    [ValidateScript({$_ | Select-String -Pattern "^[0-9a-fA-F]{64}$" -Quiet})]
    [string]$Checksum,

    [Parameter(Mandatory=$false, HelpMessage="Algorithm to use when computing zip file checksum.")]
    [ValidateNotNullOrEmpty()]
    [string]$Algorithm = "SHA256",

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
$source = [System.IO.Path]::Combine("C:\Downloads", $fileName)
$envVarValue = [System.IO.Path]::Combine($Destination, $dirName)

$ErrorActionPreference = "Stop"

if (Test-Path "$PSScriptRoot\win-installer-helper.psm1")
{
    Import-Module "$PSScriptRoot\win-installer-helper.psm1" -DisableNameChecking
}
elseif (Test-Path "$PSScriptRoot\..\..\Helpers\win-installer-helper.psm1")
{
    Import-Module "$PSScriptRoot\..\..\Helpers\win-installer-helper.psm1" -DisableNameChecking
}

Start-Setup
$PathNodes=@()
try
{
    if (-not $SkipDownload) {
        Write-Host "Downloading '$fileName' from '$Url' to '$source'"
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

    if ($Checksum)
    {
        Write-Host "Validating download..."
        $sha = Get-FileHash $Source -Algorithm $Algorithm
        Write-Host "$fileName checksum ($Algorithm): "$sha.Hash
        if (-not $sha -eq $Checksum)
        {
            Write-Error "Checksum for $Source did not match!"
            Write-Host "Expected: $Checksum"
            exit
        }
        Write-Host "Download validated successfully."
    }
    else
    {
        Write-Host "Skipping checksum validation."
    }

    if ($SkipUnzip)
    {
        Write-Host "Skipping unzip"
    }
    else
    {
        Write-Host "Expanding $Source to $destination"
        if ($PathTo7Zip)
        {
            Write-Host "***"
            Write-Host $PathTo7Zip.ToString()
            Write-Host "***"
            if (-not (Test-Path $PathTo7Zip))
            {
                Write-Error "7zip location does not exist: $PathTo7Zip"
                exit
            }
            Expand-ArchiveWith7Zip -Source $source -Destination $destination -ToolPath $PathTo7Zip
        }
        else
        {
            Expand-ArchiveWith7Zip -Source $source -Destination $destination
        }
        Write-Host "Finished unzipping to $destination"
    }
    Write-Host "Setting $EnvVar=$envVarValue"
    Add-EnvironmentVariable -Name $EnvVar -Value $envVarValue
    if ($UpdatePath)
    {
        $PathNodes += "$envVarValue\bin"
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
