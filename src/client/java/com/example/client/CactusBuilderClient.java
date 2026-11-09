package com.example.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

public class CactusBuilderClient implements ClientModInitializer {

    private static KeyMapping keyPlaceString;
    private static KeyMapping keyPlaceSand;
    private static KeyMapping keyPlaceCactus;

    @Override
    public void onInitializeClient() {
        keyPlaceString = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.cactusbuilder.string",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "category.cactusbuilder.farm"
        ));

        keyPlaceSand = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.cactusbuilder.sand",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                "category.cactusbuilder.farm"
        ));

        keyPlaceCactus = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.cactusbuilder.cactus",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "category.cactusbuilder.farm"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) return;

            long window = client.getWindow().handle();
            boolean isAltPressed = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT) ||
                                  InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);

            if (!isAltPressed) return;

            while (keyPlaceString.consumeClick()) {
                handleSmartPlacement(client, TargetType.STRING);
            }
            while (keyPlaceSand.consumeClick()) {
                handleSmartPlacement(client, TargetType.SAND);
            }
            while (keyPlaceCactus.consumeClick()) {
                handleSmartPlacement(client, TargetType.CACTUS);
            }
        });
    }

    private enum TargetType {
        STRING, SAND, CACTUS
    }

    private void handleSmartPlacement(Minecraft client, TargetType type) {
        if (client.hitResult == null || client.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult hit = (BlockHitResult) client.hitResult;
        BlockPos targetPos = hit.getBlockPos();
        Direction side = hit.getDirection();
        Block targetedBlock = client.level.getBlockState(targetPos).getBlock();
        BlockPos placePos = targetPos.relative(side);

        if (!client.level.getBlockState(placePos).isAir()) {
            return;
        }

        Item itemToUse;

        switch (type) {
            case STRING:
                if (targetedBlock != Blocks.CACTUS || !side.getAxis().isHorizontal()) {
                    return;
                }
                itemToUse = Items.STRING;
                break;

            case SAND:
                if (targetedBlock != Blocks.TRIPWIRE_HOOK && targetedBlock != Blocks.TRIPWIRE) {
                    return;
                }
                if (side != Direction.UP) {
                    return;
                }
                itemToUse = Items.SAND;
                break;

            case CACTUS:
                if ((targetedBlock != Blocks.SAND && targetedBlock != Blocks.RED_SAND) || side != Direction.UP) {
                    return;
                }
                itemToUse = Items.CACTUS;
                break;

            default:
                return;
        }

        executePlacement(client, hit, itemToUse);
    }

    private void executePlacement(Minecraft client, BlockHitResult hit, Item item) {
        Inventory inv = client.player.getInventory();
        int slot = -1;

        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).is(item)) {
                slot = i;
                break;
            }
        }

        if (slot == -1) return;

        int previousSlot = inv.getSelectedSlot();
        inv.setSelectedSlot(slot);
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
        inv.setSelectedSlot(previousSlot);
    }
}