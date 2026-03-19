$pluginsDir = "C:\Program Files\1C\1CE\components\1c-edt-2025.2.3+30-x86_64\plugins"
$jars = Get-ChildItem -Path $pluginsDir -Filter "*.jar" -File
foreach ($jar in $jars) {
    $contains = & jar tf $jar.FullName 2>$null | Select-String "RuntimeExecutionCommandBuilder"
    if ($contains) {
        Write-Host "FOUND in $($jar.FullName): $contains"
    }
}
Write-Host "Search finished."
