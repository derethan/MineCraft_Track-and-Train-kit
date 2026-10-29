$path = 'src/main/java/com/example/miningtools/client/model/LoggingCartModel.java'
$text = [System.IO.File]::ReadAllText($path)
$normalized = $text.Replace("`r`n", "`n").Replace("`n", "`r`n")
[System.IO.File]::WriteAllText($path, $normalized)
Write-Output "Normalized line endings"
