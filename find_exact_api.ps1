$pluginsDir = "C:\Program Files\1C\1CE\components\1c-edt-2025.2.3+30-x86_64\plugins"
$outFile = "C:\Users\nefedov_d\EDT_Repo1c\PluginEDT_repo1c\api_exact.txt"
Remove-Item -Path $outFile -ErrorAction SilentlyContinue

Write-Host "Searching JAR contents..."
$jars = Get-ChildItem -Path $pluginsDir -Filter "*.jar" -File
foreach ($jar in $jars) {
    try {
        $entries = & jar tf $jar.FullName 2>$null
        $matches = $entries | Where-Object { 
            $_ -like "*RuntimeExecutionCommandBuilder.class" -or 
            $_ -like "*IConfigDumpInfoStore.class" -or
            $_ -like "*IRuntimeComponentManager.class"
        }
        
        if ($matches) {
            Add-Content -Path $outFile -Value "--- JAR: $($jar.Name) ---"
            foreach ($m in $matches) {
                Add-Content -Path $outFile -Value $m
            }
        }
    } catch {
        # Ignore errors
    }
}
Write-Host "Done"
