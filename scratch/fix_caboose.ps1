$ErrorActionPreference = "Stop"
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)

$cabPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\entity\CabooseMinecartEntity.java"
$cabContent = [System.IO.File]::ReadAllText($cabPath, $utf8NoBom)
$oldCabInteract = @'
        if (player.isSecondaryUseActive()) {
            // Shift right click displays chunk anchor status
            if (!this.level().isClientSide()) {
                int count = this.getForcedChunkCount();
                Component msg = Component.literal("Caboose Chunk Anchor: ACTIVE (" + count + " Chunks Forced - Train Protected)");
                player.sendOverlayMessage(msg);
            }
            return InteractionResult.SUCCESS;
        }
'@
$newCabInteract = @'
        if (player.isSecondaryUseActive()) {
            // Shift right click opens Caboose Rename & Chunk Anchor GUI
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                int count = this.getForcedChunkCount();
                String statusStr = "Chunk Anchor: ACTIVE (" + count + " Chunks Forced)";
                String existing = this.getCustomName() != null ? this.getCustomName().getString().trim() : "";
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                        sp,
                        new com.example.miningtools.network.CartRenameOpenPayload(this.getId(), existing, "Train Caboose", statusStr)
                );
            }
            return InteractionResult.SUCCESS;
        }
'@
$cabContent = $cabContent.Replace($oldCabInteract, $newCabInteract)
[System.IO.File]::WriteAllText($cabPath, $cabContent, $utf8NoBom)
Write-Host "Updated CabooseMinecartEntity.java"
