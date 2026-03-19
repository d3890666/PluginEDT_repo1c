$jar = "C:\Program Files\1C\1CE\components\1c-edt-2025.2.3+30-x86_64\plugins\com._1c.g5.v8.dt.platform.services.core_21.0.0.v202602241426.jar"
$tmpDir = "C:\Users\nefedov_d\EDT_Repo1c\PluginEDT_repo1c\tmp_class"
$outFile = "C:\Users\nefedov_d\EDT_Repo1c\PluginEDT_repo1c\api_dump_classes.txt"

New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null
Set-Location $tmpDir

$classes = @(
    "com/_1c/g5/v8/dt/platform/services/core/runtimes/execution/RuntimeExecutionCommandBuilder.class",
    "com/_1c/g5/v8/dt/platform/services/core/runtimes/environments/IResolvableRuntimeInstallation.class",
    "com/_1c/g5/v8/dt/platform/services/core/runtimes/IRuntimeComponentManager.class",
    "com/_1c/g5/v8/dt/platform/services/core/infobases/sync/IConfigDumpInfoStore.class"
)

Remove-Item -Path $outFile -ErrorAction SilentlyContinue

foreach ($c in $classes) {
    & jar xf $jar $c 2>$null
    # Run javap on the file path
    $res = & javap -p $c 2>&1
    Add-Content -Path $outFile -Value "=== $c ==="
    Add-Content -Path $outFile -Value ($res -join "`n")
    Add-Content -Path $outFile -Value "`n"
}

Write-Host "Done"
