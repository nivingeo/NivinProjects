param ([Parameter(Mandatory)][String]$apps)

$SMP        = "D:\ibm\SMP\"
$screens    = $SMP + "maximo\tools\maximo\screen-upgrade\"


function makeDiff([String] $folder, [String]$name)
{
    $b = "-b" + $PSScriptRoot + "\diff_originals\" + $name + ".xml"
    $m = "-m" + $PSScriptRoot + "\diff_current\"   + $name + ".xml"
    $t = "-t" + $PSScriptRoot + "\diff\" + $folder + "\" + $name + ".mxs"    

    & ".\mxdiff.bat" "$b" "$m" "$t"    
}

$appList = $apps.Split(",")

Set-Location -Path $PSScriptRoot

$folder = Get-Date -format "yyyy_MM_dd"
$dst = $PSScriptRoot + "\diff\" + $folder

$suffix = 1
while((Test-Path -path ($dst + "_" + $suffix)))
{    
    $suffix++
}

$folder = $folder + "_" + $suffix


$dst = $PSScriptRoot + "\diff\" + $folder
md ($dst)

Set-Location -Path $screens

foreach ($app in $appList)
{
    makeDiff $folder $app
}

Set-Location -Path $PSScriptRoot
