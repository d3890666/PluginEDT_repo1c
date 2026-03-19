$pluginsDir = "C:\Program Files\1C\1CE\components\1c-edt-2025.2.3+30-x86_64\plugins"

Write-Host "Searching for class components..."

$jars = Get-ChildItem -Path $pluginsDir -Filter "com._1c.g5.v8.dt.*.jar" -Recurse -File

foreach ($jar in $jars) {
    $content = & jar tf $jar.FullName
    
    $builder = $content | Select-String "RuntimeExecutionCommandBuilder.class"
    if ($builder) { Write-Host "$builder found in $($jar.FullName)" }
    
    $install = $content | Select-String "IResolvableRuntimeInstallation.class"
    if ($install) { Write-Host "$install found in $($jar.FullName)" }
    
    $manager = $content | Select-String "IRuntimeComponentManager.class"
    if ($manager) { Write-Host "$manager found in $($jar.FullName)" }
    
    $store = $content | Select-String "IConfigDumpInfoStore.class"
    if ($store) { Write-Host "$store found in $($jar.FullName)" }
}
