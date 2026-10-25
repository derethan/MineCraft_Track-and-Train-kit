$path = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\WIKI.md"
$content = [System.IO.File]::ReadAllText($path)

$target = @"
### Solid Bumper Collisions & Anti-Phasing Physics
- **Anti-Phasing Collision Engine**: Vanilla Minecraft minecart collision logic suffers from a bug where carts with opposing or unaligned yaw vectors cancel push physics (`vec3.dot(vec31) < 0.8`), causing loose carts to phase straight through each other. Mining Tools overrides this with custom physical bumper separation (`TrainChainingUtil.pushMinecarts`).
- **Momentum Transfer**: When a rolling cart strikes a stationary cart, they make solid bumper contact and exchange along-track momentum smoothly, pushing the stationary cart forward realistically.
- **Coupled Car Bypassing**: Cars that are already coupled in the same consist bypass internal push collisions, ensuring long trains navigate tight switch curves without binding or jittering.
"@

$replacement = @"
### Solid Bumper Collisions, Rolling Drag & Anti-Jitter Physics
- **Rolling Stock Drag Coefficient**: When rolling freely or coasting in an unpowered consist, all rolling stock cars apply a natural drag coefficient (`Config.ROLLING_STOCK_DRAG_COEFFICIENT`, default `0.94`, 6% speed reduction per tick). Residual creeping speeds under 0.01 blocks/tick snap to zero, completely eliminating cart jittering, vibrations, and endless rolling.
- **Gentle Shunting Bump Mechanics**: Collisions between uncoupled carts use bounded momentum transfer (`Config.ROLLING_STOCK_BUMP_TRANSFER`, default `0.40`) and impact kinetic energy dissipation (`Config.ROLLING_STOCK_BUMP_DAMPING`, default `0.85`). Bumping a stationary car transfers a realistic nudge that rolls it forward several blocks to coast to a stop, rather than sending it flying down the track.
- **Anti-Phasing Collision Engine**: Vanilla Minecraft minecart collision logic suffers from a bug where carts with opposing or unaligned yaw vectors cancel push physics (`vec3.dot(vec31) < 0.8`), causing loose carts to phase straight through each other. Mining Tools overrides this with custom physical bumper separation (`TrainChainingUtil.pushMinecarts`).
- **Coupled Car Bypassing**: Cars that are already coupled in the same consist bypass internal push collisions, ensuring long trains navigate tight switch curves without binding or jittering.
"@

$content = $content.Replace($target, $replacement)
[System.IO.File]::WriteAllText($path, $content, [System.Text.UTF8Encoding]::new($false))
Write-Host "WIKI.md updated."
