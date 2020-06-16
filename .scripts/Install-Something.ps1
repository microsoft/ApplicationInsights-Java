[CmdletBinding()]
param(
    [Parameter(Mandatory,Position=0,ParameterSetName='Unzip')]
    [switch]$Unzip,
    [Parameter(Mandatory,Position=0,ParameterSetName='Installer')]
    [switch]$Install,

    [Parameter(ParameterSetName='Unzip', Mandatory)]
    [Parameter(ParameterSetName='Installer', Mandatory)]
    [ValidateNotNullOrEmpty()]
    [string]$Url,

    [Parameter(ParameterSetName='Unzip')]
    [Parameter(ParameterSetName='Installer')]
    [ValidateNotNullOrEmpty()]
    [string]$Filename=$(($Url | Select-String -Pattern ".+/(?<filename>[^/]+\..+)$").Matches[0].Groups['filename'].Value),

    [Parameter(ParameterSetName='Unzip')]
    [Parameter(ParameterSetName='Installer')]
    [ValidateScript({$_ | Select-String -Pattern "^[0-9a-fA-F]{64}$" -Quiet})]
    [string]$Checksum,

    [Parameter(ParameterSetName='Unzip')]
    [Parameter(ParameterSetName='Installer')]
    [System.IO.FileInfo]$DownloadDirectory="C:\Downloads",

    [Parameter(ParameterSetName='Unzip')]
    [Parameter(ParameterSetName='Installer')]
    [switch]$CleanOnFinish = $false,

    [Parameter(ParameterSetName='Unzip')]
    [Parameter(ParameterSetName='Installer')]
    [switch]$AddToPath = $false,

    [Parameter(ParameterSetName='Unzip')]
    [Parameter(ParameterSetName='Installer')]
    [Alias('Destination','Dest','DestinationDirectory','DestDir','DestinationDir')]
    [System.IO.FileInfo]$InstallationDirectory=$(
        if($PSCmdlet.ParameterSetName -eq 'Unzip') {
            [System.IO.Path]::GetFileNameWithoutExtension($Filename)
        }
    ),

    [Parameter(ParameterSetName='Unzip')]
    [Parameter(ParameterSetName='Installer')]
    [System.IO.FileInfo]$PathFolder, # Relative to HomeFolder

    [Parameter(ParameterSetName='Unzip')]
    [ValidateNotNullOrEmpty()]
    [System.IO.FileInfo]$PathTo7Zip,

    [Parameter(ParameterSetName='Installer')]
    [ValidateSet('EXE','MSI')]
    [string]$InstallerType='EXE',

    [Parameter(ParameterSetName='Installer')]
    [string[]]$InstallerArgs, #arguments to pass to installer

    [Parameter(ParameterSetName='Unzip')]
    [Parameter(ParameterSetName='Installer')]
    [ValidateNotNullOrEmpty()]
    [string]$HomeVar, # e.g. JAVA_HOME

    [Parameter(ParameterSetName='Unzip')]
    [Parameter(ParameterSetName='Installer')]
    [ValidateNotNullOrEmpty()]
    [string]$HomeFolder, # Relative to installation directory

    [Parameter(DontShow)]
    [Parameter(ParameterSetName='Unzip')]
    [Parameter(ParameterSetName='Installer')]
    [switch]$SkipDownload = $false,

    [Parameter(DontShow)]
    [Parameter(ParameterSetName='Unzip')]
    [Parameter(ParameterSetName='Installer')]
    [switch]$SkipInstall = $false
)

$plist = (Get-Command -Name $MyInvocation.InvocationName).Parameters
foreach ($p in $plist) {
    Get-Variable -Name $p.Values.Name -ErrorAction SilentlyContinue
}
Write-Host
Write-Host "set = " $PSCmdlet.ParameterSetName

$ErrorActionPreference = "Stop"
if (Test-Path "$PSScriptRoot\win-installer-helper.psm1")
{
    Import-Module "$PSScriptRoot\win-installer-helper.psm1" -DisableNameChecking
}
elseif (Test-Path "$PSScriptRoot\..\..\Helpers\win-installer-helper.psm1")
{
    Import-Module "$PSScriptRoot\..\..\Helpers\win-installer-helper.psm1" -DisableNameChecking
}

$DownloadedFile = [System.IO.Path]::Combine($DownloadDirectory, $Filename)
$HomeDirectory = [System.IO.Path]::Combine($InstallationDirectory, $HomeFolder)
$PathDirectory = [System.IO.Path]::Combine($HomeDirectory, $PathFolder)

Trace-Message "Creating download location $DownloadDirectory"
New-Item -Path $DownloadDirectory -ItemType Container -ErrorAction SilentlyContinue

#region function definitions
function Download-File
{
    if (-not $SkipDownload) {
        Write-Host "Downloading '$Filename' from '$Url' to '$DownloadedFile'"
        Import-Module BitsTransfer
        Start-BitsTransfer -Source $Url -Destination $DownloadedFile
        Write-Host "Download finished: $DownloadedFile"
    }
    else
    {
        Write-Host "Skipping download. Assuming $Source exists."
        if (-not (Test-Path $Source)) {
            Write-Error "$Source does not exist"
            exit
        }
    }
}

function Validate-File
{
    if ($Checksum)
    {
        Write-Host "Validating download..."
        $sha = Get-FileHash $Source
        Write-Host "$fileName checksum (SHA256): "$sha.Hash
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
}

function Create-HomeVar
{
    if ($HomeVar)
    {
        Write-Host "Setting $HomeVar=$HomeDirectory"
        Add-EnvironmentVariable -Name $HomeVar -Value $HomeDirectory
    }
}

function Extract-Zipfile
{
    Write-Host "Expanding $Source to $InstallationDirectory"
    if ($PathTo7Zip)
    {
        if (-not (Test-Path $PathTo7Zip))
        {
            Write-Error "7zip location does not exist: $PathTo7Zip"
            exit
        }
        Expand-ArchiveWith7Zip -Source $source -Destination $InstallationDirectory -ToolPath $PathTo7Zip
    }
    else
    {
        Expand-ArchiveWith7Zip -Source $source -Destination $InstallationDirectory
    }
    Write-Host "Finished unzipping to $InstallationDirectory"
}

function Run-Installer
{
    if ($InstallerType -eq 'EXE')
    {
        Install-FromEXE -Path $DownloadedFile -Arguments $InstallerArgs
    }
    elseif ($InstallerType -eq 'MSI')
    {
        Install-FromMSI -Path $DownloadedFile -Arguments $InstallerArgs
    }
}
#endregion

Start-Setup
$PathNodes=@()
try
{
    Download-File
    Validate-File

    if (-not $SkipInstall)
    {
        if ($Unzip)
        {
            Extract-Zipfile
        }
        else
        {
            Run-Installer
        }
    }
    else
    {
        Write-Host "Skipping installation."
    }

    Create-HomeVar

    if ($UpdatePath)
    {
        # TODO user homepath or pathdirectory
        $PathNodes += $PathDirectory
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
        Trace-Message "Deleting download location $DownloadDirectory"
        Remove-Item -Path $DownloadDirectory -Recurse -ErrorAction SilentlyContinue

        Stop-Setup
    }
    else
    {
        Stop-Setup -PreserveTemp -PreserveDownloads
    }
}
