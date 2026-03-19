$jar = "C:\Program Files\1C\1CE\components\1c-edt-2025.2.3+30-x86_64\plugins\com._1c.g5.v8.dt.platform.services.core_21.0.0.v202602241426.jar"
$outFile = "C:\Users\nefedov_d\EDT_Repo1c\PluginEDT_repo1c\api_dump_info.txt"

Remove-Item -Path $outFile -ErrorAction SilentlyContinue

$classes = @(
    "com/_1c/g5/v8/dt/platform/services/core/runtimes/execution/ComponentExecutorInfo.class"
)

foreach ($c in $classes) {
    & jar xf $jar $c 2>$null
    $res = & javap -p $c 2>&1
    Add-Content -Path $outFile -Value "=== $c ==="
    Add-Content -Path $outFile -Value ($res -join "`n")
    Add-Content -Path $outFile -Value "`n"
}
Write-Host "Done"
