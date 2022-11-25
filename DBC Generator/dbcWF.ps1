param ([Parameter(Mandatory)][String]$mainWF)

$SMP        = "D:\ibm\SMP\"
$internal   = $SMP + "maximo\tools\maximo\internal\"
$en         = $SMP + "maximo\tools\maximo\en\"

$eam        = "SFS\data\"
$was        = "D:\ibm\WebSphere\AppServer\profiles\ctgAppSrv01\table\"

$filePrefix = "VD-002-001."
$counter = 0;

function buildDBCTemp([String]$table, [String] $query, [ValidateNotNullOrEmpty()][String]$escape = "processname")
{
    #Remove-Item ($en + $eam + "TempRevisionList.dbc")
    Set-Location -Path $internal
    
    $fileName = "TempRevisionList"

    $t = "-t" + $table.ToUpper()
    $w = "-w" + $query
    $f = "-f" + $fileName
    $c = "-c" + $eam

    & ".\geninsertdbc.bat" "$t" "$w" "$f" "$c"
   
    Set-Location -Path $PSScriptRoot
}

function applyTemplate([String]$template, [String]$wf)
{
    echo "`n"
    echo "$template $wf"

    Set-Location -Path $PSScriptRoot
    
    $Script:counter++
    $fileName = $filePrefix + ([string]$counter).PadLeft(3,'0') + ".DEACTIVATEWF"

    $src = $PSScriptRoot + "\templates\" + $template + ".txt"
    $dst = $was + $fileName + ".dbc"
    
    (Get-Content $src) | Foreach-Object { $_.replace('$wf', $wf).replace('$rev', $rev) } | Set-Content $dst
}

function buildDBC([String]$table, [String] $query)
{
    echo ($table.PadLeft(15,' ') + ":" + $query)

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

$mainWF = $mainWF.ToUpper()

$sql = "mainprocess = '" + $mainWF + "' and revision = (select max(revision) from wfrevision where mainprocess = '" + $mainWF + "')"
buildDBCTemp "WFREVISION" $sql

$fileName = "TempRevisionList.dbc"

Set-Location -Path ($en + $eam)

[XML]$revisions = Get-Content $fileName

$process      = ""
$revision     = 0

$mainProcess  = ""
$mainRevision = 0

$wfp = [System.Collections.ArrayList]@()

foreach($insertrow in $revisions.script.statements.insert.insertrow)
{
    foreach($columnvalue in $insertrow.columnvalue)
    {
        If ($columnvalue.column -eq "MAINPROCESS")
        {
            $mainProcess = $columnvalue.string
        }
        If ($columnvalue.column -eq "PROCESSNAME")
        {
            $process = $columnvalue.string
        }
        If ($columnvalue.column -eq "PROCESSREV")
        {
            $revision = $columnvalue.number
        }
    }
    
    if ($mainProcess -eq $process)
    {
        $mainRevision = $revision

        $wfp = ,"$process,$revision" + $wfp        
    }
    else
    {
        $wfp += "$process,$revision"
    }
}

applyTemplate "DeactivateWF" $mainProcess

foreach ($theWF in $wfp)
{    
    $wf  = [String]$theWF.Split(",")[0]
    $rev = [Int]   $theWF.Split(",")[1] 

    echo "`n"
    echo "WF: $wf,$rev"

    $sql = "processname = '" + $wf + "' and processrev = " + $rev    
    buildDBC "WFPROCESS"      $sql   
    buildDBC "WFNODE"         $sql
    buildDBC "WFTASK"         $sql
    buildDBC "WFREVISION"     $sql
    buildDBC "WFSTART"        $sql
    buildDBC "WFSTOP"         $sql
    buildDBC "WFINPUT"        $sql
    buildDBC "WFACTION"       $sql
    buildDBC "WFWAITLIST"     $sql
    buildDBC "WFINTERACTION"  $sql    
    buildDBC "WFCONDITION"    $sql
    buildDBC "WFSUBPROCESS"   $sql
    buildDBC "WFNOTIFICATION" $sql        
    buildDBC "WFASGNGROUP"    $sql

    $sql = "processname = '" + $wf + "' and processrev = " + $rev + " and assignstatus = 'DEFAULT'"    
    buildDBC "WFASSIGNMENT"   $sql
           
    $sql = "templateid in (select distinct a.templateid from wfnotification a where a.processname = '" + $wf + "' and a.processrev = " + $rev + ")"       
    buildDBC "COMMTEMPLATE"    $sql
    buildDBC "COMMTMPLTSENDTO" $sql
    
    $sql =  "maxrole in (select distinct a.roleid from wfassignment a where a.processname = '" + $wf + "' and a.processrev = " + $rev + ")"
    buildDBC "MAXROLE"  $sql
                         
    $sql =  "action in (select distinct a.action from wfaction a where a.action is not null and a.processname = '" + $wf + "' and a.processrev = " + $rev + ")"
    buildDBC "ACTION"   $sql

    $sql =  "action in (select ag.member from actiongroup ag where ag.action in (select distinct a.action from WFACTION a where a.action is not null and a.processname = '" + $wf + "' and a.processrev = " + $rev + "))"
    buildDBC "ACTION"   $sql

    $sql = "action in (select distinct a.action from wfaction a where a.action is not null and a.processname = '" + $wf + "' and a.processrev = " + $rev + ")"
    buildDBC "ACTIONGROUP"   $sql
}

$appName = 'FINCNTRL'

echo "`n"
echo "APP: $appName"

$sql = "processname = '" + $mainprocess + "'"
buildDBC "WFACTIVATION" $sql

# todo: fetch appname automatically
$sql = "appname = '" + $appName + "' or processname = '" + $mainprocess + "'"
buildDBC "WFAPPTOOLBAR" $sql

$sql = "app = '" + $appName + "' and optionname in ('ASSIGNWF','HELPWF','HISTORYWF','ROUTEWF','STOPWF','VIEWWF')"
buildDBC "SIGOPTION"    $sql

$sql = "moduleapp = '" + $appName + "' and menutype = 'APPMENU' and (keyvalue IN ('ASSIGNWF','HELPWF','HISTORYWF','ROUTEWF','STOPWF','VIEWWF') or headerdescription = 'Workflow')"
buildDBC "MAXMENU"    $sql

$sql = "parent = 'FINCNTRL' and name in ('WFTOOLBAR','WORKFLOWMAP','WFTRANSACTION','WFASSIGNMENT')"
buildDBC "MAXRELATIONSHIP"    $sql

echo "`n"
echo "Other"

$sql = "persongroup in ('DETPRAPP','DETPROFF','DETPRPM','DETSUP','DETIACAD')"
buildDBC "PERSONGROUP"  $sql

$suffix = 1
$newBatch = $mainWF + "_" + $mainRevision
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