param ([Parameter(Mandatory)][String]$folder, [Parameter(Mandatory)][String]$files)

$SMP        = "D:\ibm\SMP\"
$internal   = $SMP + "maximo\tools\maximo\internal\"
$en         = $SMP + "maximo\tools\maximo\en\"
$eam        = "SFS\"

function runDBC([String] $folder, [String]$file)
{
    $file = [System.IO.Path]::GetFileNameWithoutExtension($file)

    $c = "-c" + $eam + $folder 
    $f = "-f" + $file
         
    & ".\runscriptfile.bat" "$c" "$f"
}

Set-Location -Path $PSScriptRoot

$src = $PSScriptRoot + "\import\" + $folder

$fileList = [System.Collections.ArrayList]@()
if (!($files -eq 'all'))
{
    $fileList += $files
}
else
{
    $fileList = Get-ChildItem -Path $src -Name
}

$dst = $en + $eam + $folder

if (Test-Path -path $dst)
{
    $suffix = 1
    while((Test-Path -path ($dst + "_" + $suffix)))
    {    
        $suffix++
    }

    $folder = $folder + "_" + $suffix
}

$dst = $en + $eam + $folder

md ($dst)

Set-Location -Path $src

foreach ($file in $fileList)
{
     Copy-Item $file $dst
}


Set-Location -Path $internal

foreach ($file in (Get-ChildItem $dst))
{
    runDBC $folder $file
}

Set-Location -Path $PSScriptRoot
