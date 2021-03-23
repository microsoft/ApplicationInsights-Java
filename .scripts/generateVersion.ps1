# To generate a custom version with "M.m.p.yyyymmdd<build_count_of_day>"
$path = "core\build\src\generated\main\resources\sdk-version.properties"
if (-Not (Test-Path -Path $path -PathType Leaf)) {
    Write-Error -Message "'$path' does not exist" -Category ObjectNotFound
    exit 2
}
$versionProps = ConvertFrom-StringData (Get-Content $path -Raw)
Write-Host "Read from sdk-version.properties: " $versionProps.version
$matchObj = ($versionProps.version | Select-String -Pattern "^(?<major>\d+)\.(?<minor>\d+)\.(?<patch>\d+)(?:-(?<tag>.*))?")
$major, $minor, $patch, $tag = $matchObj.Matches[0].Groups['major', 'minor','patch','tag'].Value
$date = Get-Date -UFormat "%Y%m%d"
$revision = "$date$env:CDP_DEFINITION_BUILD_COUNT_DAY"
$buildNumber = "$major.$minor.$patch.$revision"
[Environment]::SetEnvironmentVariable("CustomBuildNumber", $buildNumber, "User")  # This will allow you to use it from env var in later steps of the same phase
Write-Host "##vso[build.updatebuildnumber]${buildNumber}"