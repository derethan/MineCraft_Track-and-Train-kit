$path = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\CHANGELOG.md"
$content = [System.IO.File]::ReadAllText($path)

$oldSnippet = @"
- **Bumper-to-Bumper Rail Physics & Collision**:
  - Implemented custom minecart-to-minecart collision separation and momentum transfer (`TrainChainingUtil.pushMinecarts`), bypassing vanilla's `dot < 0.8` yaw cancellation bug.
  - Eliminates loose carts phasing through each other on tracks or when placed at stationary orientations.
  - Continuous slope height interpolation eliminating cart shaking, bouncing, and stalling when transitioning onto sloped rails.
"@

$newSnippet = @"
- **Bumper-to-Bumper Rail Physics, Rolling Drag & Anti-Jitter**:
  - Added rolling stock drag coefficient (`Config.ROLLING_STOCK_DRAG_COEFFICIENT`, default 0.94) and deadband threshold (< 0.01 b/t snaps to zero) across all rolling stock carts, eliminating cart jittering, shaking, and endless creeping.
  - Implemented bounded momentum transfer (`Config.ROLLING_STOCK_BUMP_TRANSFER`, default 0.40) and impact kinetic energy dissipation (`Config.ROLLING_STOCK_BUMP_DAMPING`, default 0.85) for uncoupled cart-to-cart bumps, preventing bumped carts from flying down the track.
  - Calibrated locomotive shunting bumper push speed to match locomotive throttle/crawl speed rather than launching carts away at high velocities.
  - Custom minecart-to-minecart bumper contact separation (`TrainChainingUtil.pushMinecarts`), bypassing vanilla's `dot < 0.8` yaw cancellation bug so loose carts never phase through each other.
  - Continuous slope height interpolation eliminating cart shaking, bouncing, and stalling when transitioning onto sloped rails.
"@

$content = $content.Replace($oldSnippet, $newSnippet)
[System.IO.File]::WriteAllText($path, $content, [System.Text.UTF8Encoding]::new($false))
Write-Host "CHANGELOG.md updated."
