package com.example.brickpalette;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.*;

public class BrickPaletteClient implements ClientModInitializer {
    public static final String STONE = "minecraft:stone_bricks";
    public static final String MOSSY = "minecraft:mossy_stone_bricks";
    public static final String CRACKED = "minecraft:cracked_stone_bricks";

    // Settings (edited in the UI). Defaults = 50 / 25 / 25.
    public static int wStone = 50, wMossy = 25, wCracked = 25;
    public static boolean connectedMode = false; // false = just the block you look at
    public static int maxBlocks = 64;            // cap for connected mode

    private static KeyMapping openKey, applyKey;
    private static final ArrayDeque<String> queue = new ArrayDeque<>();
    private static final int COMMANDS_PER_TICK = 4; // avoids the server's chat-spam kick
    private static final Random RNG = new Random();

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("brickpalette", "main"));

        // 26.3 uses SDL: take key codes from InputConstants, and Type.KEYBOARD (was KEYSYM).
        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.brickpalette.open", InputConstants.Type.KEYBOARD, InputConstants.KEY_R, category));
        applyKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.brickpalette.apply", InputConstants.Type.KEYBOARD, InputConstants.KEY_G, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) {
                if (mc.player != null) mc.gui.setScreen(new BrickPaletteScreen());
            }
            while (applyKey.consumeClick()) {
                apply();
            }
            if (mc.player != null && mc.player.connection != null) {
                for (int i = 0; i < COMMANDS_PER_TICK && !queue.isEmpty(); i++) {
                    mc.player.connection.sendCommand(queue.poll());
                }
            } else {
                queue.clear();
            }
        });
    }

    /** Replaces the block (or connected blocks) under the crosshair using the palette. */
    public static void apply() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            mc.player.sendSystemMessage(Component.literal("§cBrick Palette: not looking at a block."));
            return;
        }

        List<BlockPos> targets = connectedMode ? flood(mc, hit.getBlockPos()) : List.of(hit.getBlockPos());
        List<String> ids = pick(targets.size());
        for (int i = 0; i < targets.size(); i++) {
            BlockPos p = targets.get(i);
            queue.add("setblock " + p.getX() + " " + p.getY() + " " + p.getZ() + " " + ids.get(i));
        }
        mc.player.sendSystemMessage(Component.literal("§aBrick Palette: queued " + targets.size() + " block(s)."));
    }

    /** Same-block, face-connected region around start, capped at maxBlocks. */
    private static List<BlockPos> flood(Minecraft mc, BlockPos start) {
        Block type = mc.level.getBlockState(start).getBlock();
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        todo.add(start);
        seen.add(start);
        while (!todo.isEmpty() && out.size() < maxBlocks) {
            BlockPos p = todo.poll();
            out.add(p);
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (!seen.contains(n) && mc.level.getBlockState(n).getBlock() == type) {
                    seen.add(n);
                    todo.add(n);
                }
            }
        }
        return out;
    }

    /** Exact-ratio assignment for groups; weighted dice roll for a single block. */
    private static List<String> pick(int n) {
        int total = wStone + wMossy + wCracked;
        List<String> ids = new ArrayList<>();
        if (total <= 0) { for (int i = 0; i < n; i++) ids.add(STONE); return ids; }

        if (n == 1) {
            int r = RNG.nextInt(total);
            ids.add(r < wStone ? STONE : r < wStone + wMossy ? MOSSY : CRACKED);
            return ids;
        }
        int nStone = (int) Math.round(n * (double) wStone / total);
        int nMossy = Math.min(n - nStone, (int) Math.round(n * (double) wMossy / total));
        int nCracked = n - nStone - nMossy;
        for (int i = 0; i < nStone; i++) ids.add(STONE);
        for (int i = 0; i < nMossy; i++) ids.add(MOSSY);
        for (int i = 0; i < nCracked; i++) ids.add(CRACKED);
        Collections.shuffle(ids, RNG);
        return ids;
    }
}
