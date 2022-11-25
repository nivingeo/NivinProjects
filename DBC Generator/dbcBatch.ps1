param ([Parameter(Mandatory)][String]$autofile)

$SMP        = "D:\ibm\SMP\"
$internal   = $SMP + "maximo\tools\maximo\internal\"
$en         = $SMP + "maximo\tools\maximo\en\"

$eam        = "SFS\data\"
$was        = "D:\ibm\WebSphere\AppServer\profiles\ctgAppSrv01\table\"

$filePrefix = "VD-001-001."
$counter = 0;

function buildDBC([String]$table, [String] $query)
{
    echo ($table.PadLeft(16,' ') + ":" + $query)

    Set-Location -Path $internal
    
    $Script:counter++
    $fileName = $filePrefix + ([string]$counter).PadLeft(3,'0') + "." + $table.ToUpper()

    $s = "-sTABLE"
    $n = "-n" + $table.ToUpper()
    $q = "-w" + $query
    $f = "-f" + $fileName

    & ".\genInsertScript.bat" "$s" "$n" "$q" "$f"

    Set-Location -Path $PSScriptRoot
}

$fileName =  "list\" + $autofile + ".txt"

Set-Location -Path $PSScriptRoot

$lines = Get-Content $fileName

foreach ($line in $lines) 
{
    $table  = [String]$line.Split("|")[0]
    $where  = [String]$line.Split("|")[1] 

    buildDBC $table $where
}

$suffix = 1
$newBatch = $autofile
$dst = $PSScriptRoot + "\created\" + $newBatch

while((Test-Path -path ($dst + "_" + $suffix)))
{    
   $suffix++
}

$dst = $dst + "_" + $suffix
New-Item -ItemType "directory" -Path $dst

Set-Location -Path $was

foreach ($file in Get-ChildItem $was)
{
    Copy-Item $file $dst
    Remove-Item $file
}

Set-Location -Path $PSScriptRoot