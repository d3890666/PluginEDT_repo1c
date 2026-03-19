$edtPath = "C:\Program Files\1C\1CE\components\1c-edt-2025.2.3+30-x86_64"
$jar = Get-ChildItem "$edtPath\plugins\com._1c.g5.v8.dt.core.filesystem_*" | Select-Object -First 1 -ExpandProperty FullName
javap -cp $jar com._1c.g5.v8.dt.core.filesystem.IQualifiedNameFilePathConverter > api_converter.txt
