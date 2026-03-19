Add-Type -AssemblyName System.IO.Compression.FileSystem
$edtPath = "C:\Program Files\1C\1CE\components\1c-edt-2025.2.3+30-x86_64"
$jar = Get-ChildItem "$edtPath\plugins\com._1c.g5.v8.dt.core.filesystem_*.jar" | Select-Object -First 1 -ExpandProperty FullName
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
$zip.Entries | Where-Object { $_.FullName -like "*QualifiedName*" } | Select-Object FullName > converter_class_path.txt
$zip.Dispose()
