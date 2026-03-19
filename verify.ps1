$jar = "C:\Program Files\1C\1CE\components\1c-edt-2025.2.3+30-x86_64\plugins\dev.zigr.dt.team.ui.storage_0.3.0.202603181407.jar"
$tmpDir = "C:\Users\nefedov_d\EDT_Repo1c\PluginEDT_repo1c\tmp_verify"
Remove-Item -Recurse -Force $tmpDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $tmpDir | Out-Null
Set-Location $tmpDir
& jar xf $jar "dev/zigr/dt/team/ui/storage/Designer.class" 2>$null
& javap -c "dev/zigr/dt/team/ui/storage/Designer.class" | Select-String "getInstallation|resolveExecutor"
