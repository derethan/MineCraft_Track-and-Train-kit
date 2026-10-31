# Minecraft CubeDefinition UV formula:
# Given u0, v0, dx, dy, dz:
# Face UP (top, y-):
#   u: u0 + dz -> u0 + dz + dx
#   v: v0 -> v0 + dz
# Face DOWN (bottom, y+):
#   u: u0 + dz + dx -> u0 + dz + dx + dx
#   v: v0 -> v0 + dz
# Face WEST (x-):
#   u: u0 -> u0 + dz
#   v: v0 + dz -> v0 + dz + dy
# Face NORTH (z-):
#   u: u0 + dz -> u0 + dz + dx
#   v: v0 + dz -> v0 + dz + dy
# Face EAST (x+):
#   u: u0 + dz + dx -> u0 + dz + dx + dz
#   v: v0 + dz -> v0 + dz + dy
# Face SOUTH (z+):
#   u: u0 + dz + dx + dz -> u0 + dz + dx + dz + dx
#   v: v0 + dz -> v0 + dz + dy

function Print-BoxUV($name, $u0, $v0, $dx, $dy, $dz) {
    Write-Output "=== $name (dx=$dx, dy=$dy, dz=$dz, u0=$u0, v0=$v0) ==="
    Write-Output ("UP (Top):    U=[" + ($u0+$dz) + " .. " + ($u0+$dz+$dx) + "], V=[" + $v0 + " .. " + ($v0+$dz) + "]")
    Write-Output ("DOWN (Btm):  U=[" + ($u0+$dz+$dx) + " .. " + ($u0+$dz+$dx+$dx) + "], V=[" + $v0 + " .. " + ($v0+$dz) + "]")
    Write-Output ("WEST (End-): U=[" + $u0 + " .. " + ($u0+$dz) + "], V=[" + ($v0+$dz) + " .. " + ($v0+$dz+$dy) + "]")
    Write-Output ("NORTH (Side):U=[" + ($u0+$dz) + " .. " + ($u0+$dz+$dx) + "], V=[" + ($v0+$dz) + " .. " + ($v0+$dz+$dy) + "]")
    Write-Output ("EAST (End+): U=[" + ($u0+$dz+$dx) + " .. " + ($u0+$dz+$dx+$dz) + "], V=[" + ($v0+$dz) + " .. " + ($v0+$dz+$dy) + "]")
    Write-Output ("SOUTH(Side): U=[" + ($u0+$dz+$dx+$dz) + " .. " + ($u0+$dz+$dx+$dz+$dx) + "], V=[" + ($v0+$dz) + " .. " + ($v0+$dz+$dy) + "]")
}

Print-BoxUV "Old Log (5x5x24)" 0 40 24 5 5
Print-BoxUV "New Log (4x4x24)" 0 40 24 4 4
