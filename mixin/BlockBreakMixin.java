package me.shimmy.tlntd.mixin;

import me.shimmy.tlntd.Tlntd;
import me.shimmy.tlntd.TlntdConfig;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class BlockBreakMixin {

    @Inject(method = "breakBlock", at = @At("HEAD"))
    private void onBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        TlntdConfig cfg = TlntdConfig.load();

        Block block = mc.world.getBlockState(pos).getBlock();
        String prefix = matchBlock(block);
        if (prefix == null) return;

        // Auto-switch: bot is running and a *different* crop type was broken
        // Use switchBot() which: stops old macro → releases all keys → waits 1s → starts new
        if (cfg.cropAutoSwitch && Tlntd.isRunning && !Tlntd.activeBotType.equals(prefix)
                && !Tlntd.isSwitching) {
            cfg.lastSelectedBot = prefix;
            cfg.save();
            Tlntd.switchBot(prefix, mc);  // proper stop → delay → start
            return;
        }
        // Ignore break events during the cooldown period
        if (Tlntd.isSwitching) return;

        // Auto-start: bot is not running at all
        if (cfg.cropAutoStart && !Tlntd.isRunning) {
            cfg.lastSelectedBot = prefix;
            cfg.save();
            Tlntd.startBot(prefix, mc);
            mc.player.sendMessage(
                net.minecraft.text.Text.literal("§7[TLNTD] Auto-started: §f" + prefix), true);
        }
    }

    private String matchBlock(Block b) {
        if (b == Blocks.SUGAR_CANE)                          return "cane";
        if (b == Blocks.PUMPKIN || b == Blocks.CARVED_PUMPKIN) return "pumpkin";
        if (b == Blocks.MELON)                               return "melon";
        if (b == Blocks.NETHER_WART)                         return "wart";
        if (b == Blocks.WHEAT)                               return "wheat";
        if (b == Blocks.POTATOES)                            return "potato";
        if (b == Blocks.CARROTS)                             return "carrot";
        if (b == Blocks.RED_MUSHROOM)                        return "mushroom_red";
        if (b == Blocks.BROWN_MUSHROOM)                      return "mushroom_brown";
        if (b == Blocks.COCOA)                               return "cocoa";
        if (b == Blocks.CACTUS)                              return "cactus";
        return null;
    }
}
