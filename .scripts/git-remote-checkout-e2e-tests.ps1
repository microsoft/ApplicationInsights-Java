param (
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$RemoteName,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$RepoUrl,

    [Parameter(Mandatory = $false)]
    [ValidateNotNullOrEmpty()]
    [string]$BranchName,

    [Parameter(Mandatory = $false)]
    [ValidateNotNullOrEmpty()]
    [string]$RepoFolder,

    [Parameter(Mandatory = $false)]
    [ValidateNotNullOrEmpty()]
    [string]$PatToken
)

if ([string]::IsNullOrWhiteSpace($BranchName))
{
    Write-Host "Defaulting to checking out main branch"
    $BranchName = "main"
}

if ([string]::IsNullOrWhiteSpace($RepoFolder))
{
    Write-Host "Defaulting to checking out Git module into PSScriptRoot at $PSScriptRoot"
    $RepoFolder = $PSScriptRoot
}
else
{
    $RepoFolder = [System.IO.Path]::GetFullPath($RepoFolder)
    Write-Host "Expanded provided repository location folder into $RepoFolder"
}

if ([string]::IsNullOrWhiteSpace($PatToken))
{
    Write-Host "Defaulting to use PAT Token from environment variable $CDP_DEFAULT_CLIENT_PACKAGE_PAT"
    $PatToken = $Env:CDP_DEFAULT_CLIENT_PACKAGE_PAT
}

Write-Host "Checking out remote repository $repoUrl into $repoFolder using passed in PAT token or from environment variable."

$tokenWithUserName = ":" + $PatToken
$tokenBytes = [System.Text.UTF8Encoding]::UTF8.GetBytes($tokenWithUserName)
$encoded = [System.Convert]::ToBase64String($tokenBytes)
$headerValue = "Basic " + $encoded

&git.exe -c http.$RepoUrl.extraheader="Authorization: $headerValue" clone --verbose --progress  $RepoUrl --branch $BranchName --recurse-submodules $RepoFolder

if ($LASTEXITCODE -ne 0)
{
    Write-Error "Failed to checkout $RepoUrl branch $BranchName using CDPx default PAT token. Please contact cdpsup@microsoft.com for additional help or consult docs at aka.ms/cdpx."
    exit -1
}

Write-Host "Cloned remote Git repository $RepoUrl branch $BranchName using CDPx default PAT token into $RepoFolder"
exit 0