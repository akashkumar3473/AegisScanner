# AegisScanner Git Auto-Sync Watcher
$watchPath = 'c:\Users\HP\OneDrive\Desktop\Projects\Code Security Scanner'

$watcher = New-Object System.IO.FileSystemWatcher
$watcher.Path = $watchPath
$watcher.IncludeSubdirectories = $true
$watcher.EnableRaisingEvents = $true

Write-Host 'AegisScanner Git Auto-Sync is now active. Monitoring changes...'

$action = {
    $filePath = $Event.SourceEventArgs.FullPath
    $changeType = $Event.SourceEventArgs.ChangeType
    
    if ($filePath -match '\\\.git' -or $filePath -match '\\node_modules' -or $filePath -match '\\target' -or $filePath -match '\\reports' -or $filePath -match '\\\.system_generated' -or $filePath -match '\\watch-git\.ps1') {
        return
    }

    Write-Host 'Change detected:' $filePath '(' $changeType '). Syncing to remote...'
    Start-Sleep -Seconds 3

    try {
        git add .
        $dateStr = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
        $commitMessage = 'Auto-update: ' + $changeType + ' in codebase - ' + $dateStr
        git commit -m $commitMessage
        git push origin main
        Write-Host '✓ Push completed successfully.'
    } catch {
        Write-Host '✗ Failed to auto-push changes.'
    }
}

$handlers = @()
$handlers += Register-ObjectEvent $watcher 'Changed' -Action $action
$handlers += Register-ObjectEvent $watcher 'Created' -Action $action
$handlers += Register-ObjectEvent $watcher 'Deleted' -Action $action

try {
    while ($true) {
        Start-Sleep -Seconds 5
    }
} finally {
    foreach ($handler in $handlers) {
        Unregister-Event -SourceIdentifier $handler.Name -ErrorAction SilentlyContinue
    }
}
