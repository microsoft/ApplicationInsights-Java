$ErrorActionPreference = "Stop"

$Separator = "--------------------------------------------------------------------------------------------------------------------------------"
$DefaultDownloadFolder = "C:\Downloads"

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12


#####################################################################################################
# Start-Setup
#####################################################################################################

<#
    .SYNOPSIS
        Sets up the context for the build script to work.
    .DESCRIPTION
        Prints out disk size information and sets up the downloaded content folder.
#>
function Start-Setup
{
    Write-Host $Separator

    Trace-Message "Starting installation"

    Trace-Message "Checking disk space"
    Get-WmiObject win32_logicaldisk | Format-Table DeviceId, MediaType, {$_.Size /1GB}, {$_.FreeSpace /1GB}

    Trace-Message "Creating download location C:\Downloads"
    New-Item -Path $DefaultDownloadFolder -ItemType Container -ErrorAction SilentlyContinue
}

#####################################################################################################
# Stop-Setup
#####################################################################################################

<#
    .SYNOPSIS
        Shuts down the build script.
    .DESCRIPTION
        Deletes the downloaded content folder. Cleans the contents of the TEMP folder. Prints
        out a list of the installed software on the image by querying WMIC.
    .PARAMETER PreserveDownloads
        Preserves the downloaded content folder.
    .PARAMETER PreserveTemp
        Preserves the temp folder contents.
#>
function Stop-Setup
{
    param
    (
        [Parameter(Mandatory=$false)]
        [switch]$PreserveDownloads,

        [Parameter(Mandatory=$false)]
        [switch]$PreserveTemp
    )

    Write-Host $Separator

    if (-not $PreserveDownloads)
    {
        Trace-Message "Deleting download location C:\Downloads"
        Remove-Item -Path "C:\Downloads" -Recurse -ErrorAction SilentlyContinue
    }

    if (-not $PreserveTemp)
    {
        Reset-TempFolders
    }

    # Trace-Message "Checking disk space"
    # gwmi win32_logicaldisk | Format-Table DeviceId, MediaType, {$_.Size /1GB}, {$_.FreeSpace /1GB}

    # Trace-Message "Listing installed 32-bit software"
    # Get-ItemProperty HKLM:\Software\Wow6432Node\Microsoft\Windows\CurrentVersion\Uninstall\* | Select-Object DisplayName,DisplayVersion,Publisher,InstallDate | Sort-Object DisplayName,DisplayVersion,Publisher,InstallDate

    # Trace-Message "Listing installed 64-bit software"
    # Get-ItemProperty HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\* | Select-Object DisplayName,DisplayVersion,Publisher,InstallDate | Sort-Object DisplayName,DisplayVersion,Publisher,InstallDate

    Trace-Message "Finished installation."
    Write-Host $Separator
}

#####################################################################################################
# Get-File
#####################################################################################################

<#
    .SYNOPSIS
        Downloads a file from a URL to the downloaded contents folder.
    .DESCRIPTION
        Fetches the contents of a file from a URL to the downloaded contents folder (C:\Downloads).
        If a specific FilePath is specified, then skips the cache folder and downloads to the
        specified path.
    .PARAMETER Url
        The URL of the content to fetch.
    .PARAMETER FileName
        The name of the file to write the fetched content to.
    .OUTPUTS
        The full path to the downloaded file.
#>
function Get-File
{
    param
    (
        [Parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]
        [string]$Url,

        [Parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]
        [string]$FileName
    )

    Write-Host $Separator

    $file = [System.IO.Path]::Combine("C:\Downloads", $FileName)

    Trace-Message "Downloading from $Url to file $File"
    #Invoke-WebRequest -Uri $Url -UseBasicParsing -OutFile $file -UserAgent "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.2; .NET CLR 1.0.3705;)"
    Import-Module BitsTransfer
    Start-BitsTransfer -Source $Url -Destination $File
    #(New-Object System.Net.WebClient).DownloadFile($Url, $File)

    Trace-Message "Finished download"
    Write-Host $Separator

    return $file
}
#####################################################################################################
# Add-EnvironmentVariable
#####################################################################################################

<#
    .SYNOPSIS
        Defines a new or redefines an existing environment variable.
    .DESCRIPTION
        There are many ways to set environment variables. However, the default mechanisms do not
        work when the change has to be persisted. This implementation writes the change into
        the registry, invokes the .NET SetEnvironmentVariable method with Machine scope and then
        invokes setx /m to force persistence of the change.
    .PARAMETER Name
        The name of the environment variable.
    .PARAMETER Value
        The value of the environment variable.
    .NOTES
        This does NOT work with PATH.
#>
function Add-EnvironmentVariable
{
    param
    (
        [Parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]
        [string]$Name,

        [Parameter(Mandatory=$true)]
        [string]$Value
    )

    Write-Host $Separator

    Trace-Message "Setting environment variable $name := $value"

    Set-Item -Path Env:$Name -Value $Value
    New-Item -Path "HKLM:\System\CurrentControlSet\Control\Session Manager\Environment" -ItemType String -Force -Name $Name -Value $Value

    [System.Environment]::SetEnvironmentVariable($Name, $Value, [EnvironmentVariableTarget]::Machine)

    &setx.exe /m $Name $Value

    Write-Host $Separator
}

#####################################################################################################
#  Update-Path
#####################################################################################################

<#
    .SYNOPSIS
        Redefines the PATH.
    .DESCRIPTION
        There are many ways to set environment variables. However, the default mechanisms do not
        work when the change has to be persisted. This implementation writes the change into
        the registry, invokes the .NET SetEnvironmentVariable method with Machine scope and then
        invokes setx /m to force persistence of the change.
    .PARAMETER PathNodes
        An array of changes to the PATH. These values are appended to the existing value of PATH at the end.
    .NOTES
        This does NOT seem to work at all in Windows containers. Yet to be tested on RS5, but
        definitely did not work in RS1 through RS4.
#>
function Update-Path
{
    param
    (
        [Parameter(Mandatory=$true)]
        [string[]]$PathNodes
    )

    Write-Host $Separator

    $NodeToAppend=$null

    $path = $env:Path

    Trace-Message "Current value of PATH := $path"
    Trace-Message "Appending $Update to PATH"

    if (!$path.endswith(";"))
    {
      $path = $path + ";"
    }

    foreach ($PathNode in $PathNodes)
    {
        if (!$PathNode.endswith(";"))
        {
            $PathNode = $PathNode + ";"
        }
        $NodesToAppend += $PathNode
    }
    # add the new nodes
    $path = $path + $NodesToAppend

    #prettify it because there is some cruft from base images and or path typos i.e. foo;;
    $path = $path -replace ";+",";"

    #pull these in a hack until remove nodes is implemented
    $path = $path.Replace("C:\Program Files\NuGet;","")
    $path = $path.Replace("C:\Program Files (x86)\Microsoft Visual Studio\2019\BuildTools\MSBuild\Current\Bin;","")
    $path = $path.Replace("C:\Program Files (x86)\Microsoft Visual Studio\2019\TestAgent\Common7\IDE\CommonExtensions\Microsoft\TestWindow;","")

    #and set it
    Trace-Message "Setting PATH to $path"
    [System.Environment]::SetEnvironmentVariable("PATH", $path, [EnvironmentVariableTarget]::Machine)

    Write-Host $Separator
}


#####################################################################################################
# Add-WindowsFeature
#####################################################################################################

<#
    .SYNOPSIS
        Simple wrapper around the Install-WindowsFeature cmdlet.
    .DESCRIPTION
        A simple wrapper around the Install-WindowsFeature cmdlet that writes log lines and
        data to help trace what happened.
    .PARAMETER Name
        The name of the feature to install.

    .PARAMETER SourceString
        The full -Source parameter with location to pass into install-WindowsFeature
#>
function Add-WindowsFeature
{
    param
    (
        [Parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]
        [string]$Name,

        [Parameter(Mandatory=$false)]
        [ValidateNotNullOrEmpty()]
        [string]$SourceLocation=$null


    )

    Write-Host $Separator

    Trace-Message "Installing Windows feature $Name"

    if ($SourceLocation)
    {
      Install-WindowsFeature -Name $Name -Source $SourceLocation -IncludeAllSubFeature -IncludeManagementTools -Restart:$false -Confirm:$false
    }
    else
    {
      Install-WindowsFeature -Name $Name -IncludeAllSubFeature -IncludeManagementTools -Restart:$false -Confirm:$false
    }

    Trace-Message "Finished installing Windows feature $Name"

    Write-Host $Separator
}

#####################################################################################################
# Remove-WindowsFeature
#####################################################################################################


<#
    .SYNOPSIS
        Simple wrapper around the Uninstall-WindowsFeature cmdlet.
    .DESCRIPTION
        A simple wrapper around the Uninstall-WindowsFeature cmdlet that writes log lines and
        data to help trace what happened.
    .PARAMETER Name
        The name of the feature to uninstall.
#>
function Remove-WindowsFeature
{
    param
    (
        [Parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]
        [string]$Name
    )

    Write-Host $Separator

    Trace-Message "Removing Windows feature $Name"

    Uninstall-WindowsFeature -Name $Name -IncludeManagementTools -Restart:$false -Confirm:$false

    Trace-Message "Finished removing Windows feature $Name"

    Write-Host $Separator
}

#####################################################################################################
# Install-FromMSI
#####################################################################################################

<#
    .SYNOPSIS
        Executes a Microsoft Installer package (MSI) in quiet mode.
    .DESCRIPTION
        Uses the msiexec tool with the appropriate arguments to execute the specified installer
        package in quiet non-interactive mode with full verbose logging enabled.
    .PARAMETER Path
        The full path to the installer package file.
    .PARAMETER Arguments
        The optioal arguments to pass to the MSI installer package.
    .PARAMETER IgnoreExitCodes
        An array of exit codes to ignore. By default 3010 is always ignored because that indicates
        a restart is required. Docker layers are an implied restart. In other scenarios such as
        image builds or local runs, a restart can be easily triggered by the invoking script or
        user.
    .PARAMETER IgnoreFailures
        Flag to force all failures (including actual failing exit codes) to be ignored. Notably
        1603 is a very common one that indicates that an actual error occurred.
#>
function Install-FromMSI
{
    param
    (
        [Parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]
        [string]$Path,

        [Parameter(Mandatory=$false)]
        [string[]]$Arguments,

        [Parameter(Mandatory=$false)]
        [int[]]$IgnoreExitCodes,

        [switch]$IgnoreFailures
    )

    Write-Host $Separator

    if (-not (Test-Path $Path))
    {
        throw "CDPXERROR: Could not find the MSI installer package at $Path"
    }

    $fileNameOnly = [System.IO.Path]::GetFileNameWithoutExtension($Path)

    $log = [System.IO.Path]::Combine($env:TEMP, $fileNameOnly + ".log")

    $argsToUse = "/quiet /qn /norestart /lv! `"$log`" /i `"$Path`" $Arguments"

    Trace-Message "Installing from $Path"
    Trace-Message "Running msiexec.exe $argsToUse"

    $ex = Start-ExternalProcess -Path "msiexec.exe" -Arguments $argsToUse

    if ($ex -eq 3010)
    {
        Trace-Message "Install from $Path exited with code 3010. Ignoring since that is just indicating restart required."
        Write-Host $Separator
        return
    }
    elseif ($ex -ne 0)
    {
        foreach ($iex in $IgnoreExitCodes)
        {
            if ($ex -eq $iex)
            {
                Trace-Message "Install from $Path succeeded with exit code $ex"
                Write-Host $Separator
                return
            }
        }

        Trace-Error "Failed to install from $Path. Process exited with code $ex"

        if (-not $IgnoreFailures)
        {
            throw "Failed to install from $Path. Process exited with code $ex"
        }
    }
}

#####################################################################################################
# Install-FromEXE
#####################################################################################################

<#
    .SYNOPSIS
        Executes any arbitrary executable installer.
    .DESCRIPTION
        A simple wrapper function to kick off an executable installer and handle failures, logging etc.
    .PARAMETER Path
        The path to the installer package file.
    .PARAMETER Arguments
        The optioal arguments to pass to the installer package.
    .PARAMETER IgnoreExitCodes
        An array of exit codes to ignore. By default 3010 is always ignored because that indicates
        a restart is required. Docker layers are an implied restart. In other scenarios such as
        image builds or local runs, a restart can be easily triggered by the invoking script or
        user.
    .PARAMETER IgnoreFailures
        Flag to force all failures (including actual failing exit codes) to be ignored. Notably
        1603 is a very common one that indicates that an actual error occurred.
#>
function Install-FromEXE
{
    param
    (
        [Parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]
        [string]$Path,

        [Parameter(Mandatory=$false)]
        [int[]]$IgnoreExitCodes,

        [Parameter(Mandatory=$false)]
        [string[]]$Arguments,

        [switch]$IgnoreFailures
    )

    Write-Host $Separator

    Trace-Message "Running $Path"

    $ex = Start-ExternalProcess -Path $Path -Arguments $Arguments

    if ($ex -eq 3010)
    {
        Trace-Message "Install from $Path exited with code 3010. Ignoring since that is just indicating restart required."
        Write-Host $Separator
        return
    }
    elseif ($ex -ne 0)
    {
        foreach ($iex in $IgnoreExitCodes)
        {
            if ($ex -eq $iex)
            {
                Trace-Message "Install from $Path succeeded with exit code $ex"
                Write-Host $Separator
                return
            }
        }

        Trace-Error "Failed to install from $Path. Process exited with code $ex"

        if (-not $IgnoreFailures)
        {
            throw "Failed to install from $Path. Process exited with code $ex"
        }
    }
}

#####################################################################################################
# Install-FromInnoSetup
#####################################################################################################

<#
    .SYNOPSIS
        A shorthand function for running a Inno Setup installer package with the appropriate options.
    .DESCRIPTION
        Inno Setup installer packages can be run in silent mode with the options
        /VERYSILENT /NORESTART /CLOSEAPPLICATIONS /TYPE=full. In most cases, these options are the
        same for every Inno Setup installer. This function is hence a short hand for Inno Setup.
    .PARAMETER Path
        The path to the Inno Setup installer package file.
    .PARAMETER Arguments
        The optioal arguments to pass to the installer package.
    .PARAMETER IgnoreExitCodes
        An array of exit codes to ignore.
    .PARAMETER IgnoreFailures
        Flag to force all failures (including actual failing exit codes) to be ignored.

#>
function Install-FromInnoSetup
{
    param
    (
        [Parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]
        [string]$Path,

        [Parameter(Mandatory=$false)]
        [int[]]$IgnoreExitCodes,

        [Parameter(Mandatory=$false)]
        [string[]]$Arguments,

        [switch]$IgnoreFailures
    )

    $fileNameOnly = [System.IO.Path]::GetFileNameWithoutExtension($Path)
    $logName = $fileNameOnly + ".log"
    $logFile = Join-Path $Env:TEMP -ChildPath $logName

    $argsToUse = "/QUIET /SP- /VERYSILENT /SUPPRESSMSGBOXES /NORESTART /CLOSEAPPLICATIONS /NOICONS /TYPE=full /LOG `"$logFile`" "
    $argsToUse += $Arguments

    Install-FromEXE -Path $Path -Arguments $argsToUse -IgnoreExitCodes $IgnoreExitCodes -IgnoreFailures:$IgnoreFailures
}

#####################################################################################################
# Install-FromDevToolsInstaller
#####################################################################################################

<#
    .SYNOPSIS
        A shorthand function for running a DevDiv Tools installer package with the appropriate options.
    .DESCRIPTION
        DevDiv Tools installer packages can be run in silent mode with the options
        /quiet /install /norestart. In most cases, these options are the
        same for every DevDiv Tools installer. This function is hence a short hand for DevDiv Tools
        installer packages.
    .PARAMETER Path
        The path to the DevDiv Tools installer package file.
    .PARAMETER Arguments
        The optional arguments to pass to the installer package.
    .PARAMETER IgnoreExitCodes
        An array of exit codes to ignore. 3010 is added by default by this function.
    .PARAMETER IgnoreFailures
        Flag to force all failures (including actual failing exit codes) to be ignored.

#>
function Install-FromDevDivToolsInstaller
{
    param
    (
        [Parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]
        [string]$Path,

        [Parameter(Mandatory=$false)]
        [int[]]$IgnoreExitCodes,

        [Parameter(Mandatory=$false)]
        [string[]]$Arguments,

        [switch]$IgnoreFailures
    )

    $fileNameOnly = [System.IO.Path]::GetFileNameWithoutExtension($Path)
    $logName = $fileNameOnly + ".log"
    $logFile = Join-Path $Env:TEMP -ChildPath $logName

    $argsToUse = "/QUIET /INSTALL /NORESTART `"$logFile`" "
    $argsToUse += $Arguments

    $iec = (3010)
    $iec += $IgnoreExitCodes

    Install-FromEXE -Path $Path -Arguments $argsToUse -IgnoreExitCodes $iec -IgnoreFailures:$IgnoreFailures
}

#####################################################################################################
# Install-FromChocolatey
#####################################################################################################

<#
    .SYNOPSIS
        Installs a Chocolatey package.
    .DESCRIPTION
        Installs a package using Chocolatey in silent mode with no prompts.
    .PARAMETER Name
        The name of the package to install.

#>
function Install-FromChocolatey
{
    param
    (
        [Parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]
        [string]$Name
    )

    Write-Host $Separator

    Write-Host "Installing chocolatey package $Name"
    Start-ExternalProcess -Path "C:\ProgramData\chocolatey\bin\choco.exe" -Arguments @("install","-y",$Name)

    Write-Host $Separator
}


#####################################################################################################
# Install-FromEXEAsyncWithDevenvKill
#####################################################################################################

<#
    .SYNOPSIS
        Starts an installer asynchronously and waits in the background for rogue child processes
        and kills them after letting them finish.
    .DESCRIPTION
        Visual Studio installers start a number of child processes. Notable amongst them is the devenv.exe
        process that attempts to initialize the VS IDE. Containers do not support UIs so this part hangs.
        There might be other related processes such as msiexec as well that hang. Invariable, these
        child processes complete quite fast, but never exit potentially becuase they are attempting
        to display some UI and hang. This helper function will kick off the installer and then monitor
        the task list to find those child processes by name and then it will kill them.
    .PARAMETER Path
    .PARAMETER StuckProcessNames
    .PARAMETER IgnoreExitCodes
    .PARAMETER IgnoreFailures
    .PARAMETER Arguments
    .PARAMETER WaitMinutes
#>
function Install-FromEXEAsyncWithDevenvKill
{
    param
    (
        [Parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]
        [string]$Path,

        [Parameter(Mandatory=$true)]
        [string[]]$StuckProcessNames,

        [Parameter(Mandatory=$false)]
        [int[]]$IgnoreExitCodes,

        [Parameter()]
        [switch]$IgnoreFailures,

        [Parameter(Mandatory=$false)]
        [ValidateRange(1, [int]::MaxValue)]
        [int]$WaitMinutes = 5,

        [string[]]$Arguments
    )

    Write-Host $Separator

    Trace-Message "Running $Path with $Arguments"

    $process = Start-Process $Path -PassThru -Verbose -NoNewWindow -ArgumentList $Arguments
    $thePid = $process.Id
    $pn = [System.IO.Path]::GetFileNameWithoutExtension($Path)

    Trace-Message "Started EXE asynchronously. Process ID is $thePid"

    Wait-ForProcess -Process $process -Minutes $WaitMinutes

    Trace-Message "Walking task list and killing any processes in the stuck process list $StuckProcessNames"

    foreach ($stuckProcessName in $StuckProcessNames)
    {
        Stop-ProcessByName -Name $stuckProcessName -WaitBefore 3 -WaitAfter 3
    }

    Trace-Message "Also killing any rogue msiexec processes"

    Stop-ProcessByName -Name "msiexec" -WaitBefore 3 -WaitAfter 3

    Wait-WithMessage -Message "Waiting for process with ID $thePid launched from $Path to finish now that children have been killed off" -Minutes 2

    Stop-ProcessByName -Name $pn -WaitBefore 3 -WaitAfter 3

    $ex = $process.ExitCode;

    if ($ex -eq 0)
    {
        Trace-Message "Install from $Path succeeded with exit code 0"
        Write-Host $Separator
        return
    }

    foreach ($iex in $ignoreExitCodes)
    {
        if ($ex -eq $iex)
        {
            Trace-Message "Install from $Path succeeded with exit code $ex"
            Write-Host $Separator
            return;
        }
    }

    Trace-Error "Failed to install from $Path. Process exited with code $ex"

    if (-not $IgnoreFailures)
    {
        throw "CDPXERROR: Failed to install from $Path. Process exited with exit code $ex"
    }
}
#####################################################################################################
# Stop-ProcessByName
#####################################################################################################

<#
    .SYNOPSIS
        Kills all processes with a given name.
    .DESCRIPTION
        Some installers start multiple instances of other applications to perform various
        post-installer or initialization actions. The most notable is devenv.exe. This function
        provides a mechanism to brute force kill all such instances.
    .PARAMETER Name
        The name of the process to kill.
    .PARAMETER WaitBefore
        The optional number of minutes to wait before killing the process. This provides time for
        the process to finish its processes.
    .PARAMETER WaitAfter
        The optional number of minutes to wait after killing the process. This provides time for
        the process to exit and any handles to expire.
#>
function Stop-ProcessByName
{
    param
    (
        [Parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]
        [string]$Name,

        [Parameter(Mandatory=$false)]
        [ValidateRange(1, [int]::MaxValue)]
        [int]$WaitBefore = 3,

        [Parameter(Mandatory=$false)]
        [ValidateRange(1, [int]::MaxValue)]
        [int]$WaitAfter = 3
    )

    Wait-WithMessage -Message "Waiting for $WaitBefore minutes before killing all processes named $processName" -Minutes $WaitBefore
    &tasklist /v

    $count = 0

    Get-Process -Name $Name -ErrorAction SilentlyContinue |
        ForEach-Object
        {
            $process = $_
            Trace-Warning "Killing process with name $Name and ID $($process.Id)"
            $process.Kill()
            ++$count
        }

    Trace-Warning "Killed $count processes with name $Name"

    Wait-WithMessage -Message "Waiting for $WaitAfter minutes after killing all processes named $Name" -Minutes $WaitAfter

    &tasklist /v
}

#####################################################################################################
# Wait-WithMessage
#####################################################################################################

<#
    .SYNOPSIS
        Performs a synchronous sleep.
    .DESCRIPTION
        Some asynchronous and other operations require a wait time before
        assuming a failure. This function forces the caller to sleep. The sleep is
        performed in 1-minute intervals and a message is printed on each wakeup.
    .PARAMETER Message
        The message to print after each sleep period.
    .PARAMETER Minutes
        The number of minutes to sleep.
#>
function Wait-WithMessage
{
    param
    (
        [Parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]
        [string]$Message,

        [Parameter(Mandatory=$true)]
        [ValidateRange(1, [int]::MaxValue)]
        [int]$Minutes
    )

    $elapsed = 0

    while ($true)
    {
        if ($elapsed -ge $Minutes)
        {
            Write-Host "Done waiting for $elapsed minutes"
            break
        }

        Trace-Message $Message
        Start-Sleep -Seconds 60
        ++$elapsed
    }
}


#####################################################################################################
# Wait-WithMessageAndMonitor
#####################################################################################################

<#
    .SYNOPSIS
        Performs a synchronous sleep and on each wakeup runs a script block that may contain some
        monitoring code.
    .DESCRIPTION
        Some asynchronous and other operations require a wait time before
        assuming a failure. This function forces the caller to sleep. The sleep is performed
        in 1-minute intervals and a message is printed and a script block is run on each wakeup.
    .PARAMETER Message
        The message to print after each sleep period.
    .PARAMETER Block
        The script block to run after each sleep period.
    .PARAMETER Minutes
        The number of minutes to sleep.
#>
function Wait-WithMessageAndMonitor
{
    param
    (
        [Parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]
        [string]$Message,

        [Parameter(Mandatory=$true)]
        [ValidateNotNull()]
        [ScriptBlock]$Monitor,

        [Parameter(Mandatory=$true)]
        [ValidateRange(1, [int]::MaxValue)]
        [int]$Minutes
    )

    $elapsed = 0

    while ($true)
    {
        if ($elapsed -ge $Minutes)
        {
            Write-Host "Done waiting for $elapsed minutes"
            break
        }

        Trace-Message $Message
        Start-Sleep -Seconds 60
        $Monitor.Invoke()
        ++$elapsed
    }
}

#####################################################################################################
# Reset-TempFolders
#####################################################################################################

<#
    .SYNOPSIS
        Deletes the contents of well known temporary folders.
    .DESCRIPTION
        Installing lots of software can leave the TEMP folder built up with crud. This function
        wipes the well known temp folders $Env:TEMP and C:\Windows\TEMP of all contentes. The
        folders are preserved however.
#>
function Reset-TempFolders
{
    try
    {
        Trace-Message "Wiping contents of the $($Env:TEMP) and C:\Windows\TEMP folders."

        Get-ChildItem -Directory -Path $Env:TEMP |  ForEach-Object {
                $p = $_.FullName
                Trace-Message "Removing temporary file $p"
                Remove-Item -Recurse -Force -Path $p -ErrorAction SilentlyContinue
            }

        Get-ChildItem -File -Path $Env:TEMP | ForEach-Object {
                $p = $_.FullName
                Trace-Message "Removing temporary file $p"
                Remove-Item -Force -Path $_.FullName -ErrorAction SilentlyContinue
            }

        Get-ChildItem -Directory -Path "C:\Windows\Temp" | ForEach-Object {
                $p = $_.FullName
                Trace-Message "Removing temporary file $p"
                Remove-Item -Recurse -Force -Path $_.FullName -ErrorAction SilentlyContinue
            }

        Get-ChildItem -File -Path "C:\Windows\Temp" | ForEach-Object {
                $p = $_.FullName
                Trace-Message "Removing temporary file $p"
                Remove-Item -Force -Path $_.FullName -ErrorAction SilentlyContinue
            }
    }
    catch
    {
        Trace-Warning "Errors occurred while trying to clean up temporary folders."
        $_.Exception | Format-List
    }
    finally
    {
        Trace-Message "Cleaned up temporary folders at $Env:TEMP and C:\Windows\Temp"
    }
}
#####################################################################################################
# Confirm-FileHash
#####################################################################################################

<#
    .SYNOPSIS
        Verifies the content hash of downloaded content.
    .DESCRIPTION
        By default computes the SHA256 hash of downloaded content and compares it against
        a given hash assuming it to be a SHA256 hash as well.
    .PARAMETER FileName
        The name of the file. If the IsFullPath switch is not specified, assumes a file within
        the downloaded content cache.
    .PARAMETER ExpectedHash
        The expected hash value of the content.
    .PARAMETER Algorithm
        The optional hash algorithm to hash. Defaults to SHA256.
    .OUTPUTS
#>
function Confirm-FileHash
{
    param
    (
        [Parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]
        [string]$Path,

        [Parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]
        [string]$ExpectedHash,

        [Parameter(Mandatory=$false)]
        [ValidateNotNullOrEmpty()]
        [string]$Algorithm = "sha256"
    )

    Trace-Message "Verifying content hash for file $Path"

    $exists = Test-Path -Path $Path -PathType Leaf

    if (-not $exists)
    {
        throw "CDPXERROR: Failed to find file $Path in order to verify hash."
    }

    $hash = Get-FileHash $Path -Algorithm $Algorithm

    if ($hash.Hash -ne $ExpectedHash)
    {
        throw "File $Path hash $hash.Hash did not match expected hash $expectedHash"
    }
}

#####################################################################################################
# Start-ExternalProcess
#####################################################################################################

<#
    .SYNOPSIS
        Executes an external application
    .DESCRIPTION
        PowerShell does not deal well with applications or scripts that write to
        standard error. This wrapper function handles starting the process,
        waiting for output and then captures the standard output/error streams and
        reports them without writing them to stderr.
    .PARAMETER Path
        The path to the application to run.
    .PARAMETER Arguments
        The array of arguments to pass to the external application.
    .OUTPUTS
        Returns the exit code that the application exited with.
#>
function Start-ExternalProcess
{
    param
    (
        [Parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]
        [string]$Path,

        [Parameter(Mandatory=$false)]
        [string[]]$Arguments
    )

    Trace-Message "Executing application: $Path $Arguments"

    $guid = [System.Guid]::NewGuid().ToString("N")
    $errLogFileName = -join($guid, "-stderr.log")
    $outLogFileName = -join($guid, "-stdout.log")
    $errLogFile = Join-Path -Path $Env:TEMP -ChildPath $errLogFileName
    $outLogFile = Join-Path -Path $Env:TEMP -ChildPath $outLogFileName
    [System.Diagnostics.Process]$process = $null

    if (($null -ne $Arguments) -and ($Arguments.Length -gt 0))
    {
        $process = Start-Process -FilePath $Path -ArgumentList $Arguments -NoNewWindow -PassThru -RedirectStandardError $errLogFile -RedirectStandardOutput $outLogFile
    }
    else
    {
        $process = Start-Process -FilePath $Path -NoNewWindow -PassThru -RedirectStandardError $errLogFile -RedirectStandardOutput $outLogFile
    }

    $handle = $process.Handle
    $thePid = $process.Id
    $ex = 0

    Trace-Message -Message "Started process from $Path with PID $thePid (and cached handle $handle)"

    while ($true)
    {
        Trace-Message -Message "Waiting for PID $thePid to exit ..."

        if ($process.HasExited)
        {
            Trace-Message -Message "PID $thePid has exited!"
            break
        }

        Start-Sleep -Seconds 60
    }

    Trace-Message "STDERR ---------------------------"
    Get-Content $errLogFile | Write-Host

    Trace-Message "STDOUT ---------------------------"
    Get-Content $outLogFile | Write-Host

    $ex = $process.ExitCode

    if ($null -eq $ex)
    {
        Trace-Warning -Message "The process $thePid returned a null or invalid exit code value. Assuming and returning 0"
        $ex = 0
    }
    else
    {
        Trace-Message "Process $thePid exited with exit code $ex"
    }

    return $ex
}

#####################################################################################################
# Invoke-ExternalProcessWithWaitAndKill
#####################################################################################################

<#
    .SYNOPSIS
        Executes an external application, waits for a specified amount of time and then kills it.
    .DESCRIPTION
        Some applications get stuck when running for the first time. This function starts the
        application, then waits and then kills it so that a subsequent run can succeed.
    .PARAMETER Path
        The path to the application to run.
    .PARAMETER Arguments
        The array of arguments to pass to the external application.
    .PARAMETER Minutes
        The amount of time to wait in minutes before killing the external application.
    .OUTPUTS
        The exit code if one is available from the process.
#>
function Invoke-ExternalProcessWithWaitAndKill
{
    param
    (
        [Parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]
        [string]$Path,

        [Parameter(Mandatory=$false)]
        [string[]]$Arguments,

        [Parameter(Mandatory=$false)]
        [ScriptBlock]$Monitor,

        [Parameter(Mandatory=$false)]
        [ValidateRange(1, [int]::MaxValue)]
        [int]$Minutes
    )

    Trace-Message "Executing application: $Path $Arguments. Will wait $Minutes minutes before killing it."

    $guid = [System.Guid]::NewGuid().ToString("N")
    $errLogFileName = -join($guid, "-stderr.log")
    $outLogFileName = -join($guid, "-stdout.log")
    $errLogFile = Join-Path -Path $Env:TEMP -ChildPath $errLogFileName
    $outLogFile = Join-Path -Path $Env:TEMP -ChildPath $outLogFileName
    [System.Diagnostics.Process]$process = $null

    if (-not $Arguments)
    {
        $process = Start-Process -FilePath $Path -NoNewWindow -PassThru -RedirectStandardError $errLogFile -RedirectStandardOutput $outLogFile
    }
    else
    {
        $process = Start-Process -FilePath $Path -ArgumentList $Arguments -NoNewWindow -PassThru -RedirectStandardError $errLogFile -RedirectStandardOutput $outLogFile
    }

    $handle = $process.Handle
    $thePid = $process.Id
    $ex = 0

    Trace-Message -Message "Started process from $Path with PID $thePid (and cached handle $handle)"

    $exited = Wait-ForProcess -Process $process -Minutes $Minutes -Monitor $Monitor

    if (-not $exited)
    {
        Trace-Warning "CDPXERROR: Process with ID $thePid failed to exit within $Minutes minutes. Killing it."

        try
        {
            $process.Kill()
            Trace-Warning "Killed PID $thePid"
        }
        catch
        {
            Trace-Warning "Exception raised while attempting to kill PID $thePid. Perhaps the process has already exited."
            $_.Exception | Format-List
        }
    }
    else
    {
        $ex = $process.ExitCode
        Trace-Message "Application $Path exited with exit code $ex"
    }

    Trace-Message "STDERR ---------------------------"
    Get-Content $errLogFile | Write-Host

    Trace-Message "STDOUT ---------------------------"
    Get-Content $outLogFile | Write-Host

    if ($null -eq $ex)
    {
        Trace-Warning -Message "The process $thePid returned a null or invalid exit code value. Assuming and returning 0"
        return 0
    }

    return $ex
}

#####################################################################################################
# Wait-ForProcess
#####################################################################################################

<#
    .SYNOPSIS
        Waits for a previously started process until it exits or there is a timeout.
    .DESCRIPTION
        Waits for a started process until it exits or a certain amount of time has elapsed.
    .PARAMETER Process
        The [System.Process] project to wait for.
    .PARAMETER Minutes
        The amount of time to wait for in minutes.
    .PARAMETER Monitor
        An optional script block that will be run after each wait interval.
#>
function Wait-ForProcess
{
    param
    (
        [Parameter(Mandatory=$true)]
        [ValidateNotNull()]
        [System.Diagnostics.Process]$Process,

        [Parameter(Mandatory=$true)]
        [ValidateRange(1, [int]::MaxValue)]
        [int]$Minutes,

        [Parameter(Mandatory=$false)]
        [ScriptBlock]$Monitor
    )

    $waitTime = $Minutes

    $handle = $process.Handle
    $thePid = $Process.Id

    while ($waitTime -gt 0)
    {
        Trace-Message -Message "Waiting for process with ID $thePid to exit in $waitTime minutes (handle: $handle) ."

        if ($Process.HasExited)
        {
            $ex = $Process.ExitCode
            Trace-Message "Process with ID $thePid has already exited with exit code $ex"
            return $true
        }

        Start-Sleep -Seconds 60

        if ($Monitor)
        {
            try
            {
                Trace-Message "Invoking monitor script: $Monitor"
                $Monitor.Invoke()
            }
            catch
            {
                Trace-Warning "Exception occurred invoking monitoring script"
                $_.Exception | Format-List
            }
        }

        --$waitTime
    }

    return $false
}

#####################################################################################################
# Trace-Message
#####################################################################################################

<#
    .SYNOPSIS
        Logs an informational message to the console.
    .DESCRIPTION
        Writes a message to the console with the current timestamp and an information tag.
    .PARAMETER Message
        The message to write.
#>
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

#####################################################################################################
# Trace-Warning
#####################################################################################################

<#
    .SYNOPSIS
        Logs a warning message to the console.
    .DESCRIPTION
        Writes a warning to the console with the current timestamp and a warning tag.
    .PARAMETER Message
        The warning to write.
#>
function Trace-Warning
{
    param
    (
        [Parameter(Mandatory=$true, Position=0)]
        [ValidateNotNullOrEmpty()]
        [string]$Message
    )

    $timestamp = Get-Date
    $Message = $Message -replace "##vso", "__VSO_DISALLOWED"
    Write-Host "[WARN] [$timestamp] $Message" -ForegroundColor Yellow
    Write-Host "##vso[task.logissue type=warning]$Message"
}

#####################################################################################################
# Trace-Error
#####################################################################################################

<#
    .SYNOPSIS
        Logs an error message to the console.
    .DESCRIPTION
        Writes an error to the console with the current timestamp and an error tag.
    .PARAMETER Message
        The error to write.
#>
function Trace-Error
{
    param
    (
        [Parameter(Mandatory=$true, Position=0)]
        [ValidateNotNullOrEmpty()]
        [string]$Message
    )

    $timestamp = Get-Date
    $Message = $Message -replace "##vso", "__VSO_DISALLOWED"
    Write-Host "[ERROR] [$timestamp] $Message" -ForegroundColor Red
    Write-Host "##vso[task.logissue type=error]$Message"
}

#####################################################################################################
# Expand-ArchiveWith7Zip
#####################################################################################################

<#
    .SYNOPSIS
        Uses 7-Zip to expand an archive instead of the standard Expand-Archive cmdlet.
    .DESCRIPTION
        The Expand-Archive cmdlet is slow compared to using 7-Zip directly. This function
        assumes that 7-Zip is installed at C:\7-Zip.
    .PARAMETER -Source
        The path to the archive file.
    .PARAMETER -Destination
        The folder to expand into.
    .PARAMETER ToolPath
        The path to where the 7z.exe tool is available.
#>
function Expand-ArchiveWith7Zip
{
    param
    (
        [Parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]
        [string]$Source,

        [Parameter(Mandatory=$false)]
        [ValidateNotNullOrEmpty()]
        [string]$Destination,

        [Parameter(Mandatory=$false)]
        [ValidateNotNullOrEmpty()]
        [string]$ToolPath = "C:\7-Zip\7z.exe",

        [Parameter(Mandatory=$false)]
        [switch]$IgnoreFailures=$false
    )

    Write-Host $Separator

    if (-not $ToolPath)
    {
        throw "CDPXERROR: The 7-Zip tool was not found at $ToolPath."
    }

    if (-not (Test-Path $Source))
    {
        throw "CDPXERROR: The specified archive file $Source could not be found."
    }

    if (-not $Destination)
    {
        $sourceDir = [System.IO.Path]::GetDirectoryName($Source);
        $Destination = $sourceDir

        Trace-Message "No destination was specified so the default location $Destination was chosen."
    }

    Trace-Message "Uncompressing archive $Source into folder $Destination using 7-Zip at $ToolPath"

    Install-FromEXE -Path $ToolPath -Arguments "x -aoa -y `"$Source`" -o`"$Destination`"" -IgnoreFailures:$IgnoreFailures

    Trace-Message "Successfully uncompressed archive at $Source into $Destination"
    Write-Host $Separator
}
