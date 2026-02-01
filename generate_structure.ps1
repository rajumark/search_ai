$target = "app\src\main"
$outFile = "project_structure.json"

function Get-Structure($path) {
    # Using Ordered dictionary to keep things somewhat tidy, though JSON order isn't guaranteed
    $result = [ordered]@{} 
    
    # Get items, sort by name for deterministic output
    $items = Get-ChildItem -Path $path -ErrorAction SilentlyContinue | Sort-Object Name
    
    foreach ($item in $items) {
        if ($item.PSIsContainer) {
            $result[$item.Name] = Get-Structure $item.FullName
        } else {
            $result[$item.Name] = "file"
        }
    }
    return $result
}

if (Test-Path $target) {
    Write-Host "Scanning $target..."
    $structure = Get-Structure $target
    # Depth 20 should be enough for most projects
    $structure | ConvertTo-Json -Depth 20 | Out-File -FilePath $outFile -Encoding UTF8
    Write-Host "Successfully created $outFile"
} else {
    Write-Host "Target $target not found in $PWD"
}
