package com.example.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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

public class CactusBuilderClient implements ClientModInitializer {

    // GLFW Tuş Kodları
    private static final int KEY_Z = 90;
    private static final int KEY_X = 88;
    private static final int KEY_C = 67;
    private static final int KEY_LEFT_ALT = 342;
    private static final int KEY_RIGHT_ALT = 346;

    private static boolean wasZPressed = false;
    private static boolean wasXPressed = false;
    private static boolean wasCPressed = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Menüdeyken veya chat açıkken tuşların tetiklenmesini engelle
            if (client.screen != null || client.player == null || client.level == null) {
                return;
            }

            boolean isAltPressed = InputConstants.isKeyDown(KEY_LEFT_ALT) || InputConstants.isKeyDown(KEY_RIGHT_ALT);
            if (!isAltPressed) {
                wasZPressed = false;
                wasXPressed = false;
                wasCPressed = false;
                return;
            }

            boolean isZDown = InputConstants.isKeyDown(KEY_Z);
            boolean isXDown = InputConstants.isKeyDown(KEY_X);
            boolean isCDown = InputConstants.isKeyDown(KEY_C);

            // Alt + Z -> İp yerleştir
            if (isZDown && !wasZPressed) {
                handleSmartPlacement(client, TargetType.STRING);
            }
            // Alt + X -> Kum yerleştir
            if (isXDown && !wasXPressed) {
                handleSmartPlacement(client, TargetType.SAND);
            }
            // Alt + C -> Kaktüs yerleştir
            if (isCDown && !wasCPressed) {
                handleSmartPlacement(client, TargetType.CACTUS);
            }

            wasZPressed = isZDown;
            wasXPressed = isXDown;
            wasCPressed = isCDown;
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