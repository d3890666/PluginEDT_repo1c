$jar = "C:\Program Files\1C\1CE\components\1c-edt-2025.2.3+30-x86_64\plugins\com._1c.g5.v8.dt.platform.services.core_21.0.0.v202602241426.jar"
$tmpDir = "C:\Users\nefedov_d\EDT_Repo1c\PluginEDT_repo1c\tmp_class_sync"
$outFile = "C:\Users\nefedov_d\EDT_Repo1c\PluginEDT_repo1c\api_dump_sync.txt"
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null
Set-Location $tmpDir

& jar xf $jar "com/_1c/g5/v8/dt/platform/services/core/infobases/sync/" 2>$null
$files = Get-ChildItem -Path "com/_1c/g5/v8/dt/platform/services/core/infobases/sync/" -Filter "*.class" -Recurse

Remove-Item -Path $outFile -ErrorAction SilentlyContinue

foreach ($file in $files) {
    $res = & javap -p $file.FullName 2>&1
    Add-Content -Path $outFile -Value "=== $($file.Name) ==="
    Add-Content -Path $outFile -Value ($res -join "`n")
    Add-Content -Path $outFile -Value "`n"
}
Write-Host "Done"
