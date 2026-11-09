package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

public class CactusBuilderClient implements ClientModInitializer {

    private static KeyBinding keyPlaceString;
    private static KeyBinding keyPlaceSand;
    private static KeyBinding keyPlaceCactus;

    @Override
    public void onInitializeClient() {
        keyPlaceString = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cactusbuilder.string",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "category.cactusbuilder.farm"
        ));

        keyPlaceSand = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cactusbuilder.sand",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                "category.cactusbuilder.farm"
        ));

        keyPlaceCactus = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cactusbuilder.cactus",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "category.cactusbuilder.farm"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            long window = client.getWindow().getHandle();
            boolean isAltPressed = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_ALT) ||
                                  InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_ALT);

            if (!isAltPressed) return;

            while (keyPlaceString.wasPressed()) {
                handleSmartPlacement(client, TargetType.STRING);
            }
            while (keyPlaceSand.wasPressed()) {
                handleSmartPlacement(client, TargetType.SAND);
            }
            while (keyPlaceCactus.wasPressed()) {
                handleSmartPlacement(client, TargetType.CACTUS);
            }
        });
    }

    private enum TargetType {
        STRING, SAND, CACTUS
    }

    private void handleSmartPlacement(MinecraftClient client, TargetType type) {
        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult hit = (BlockHitResult) client.crosshairTarget;
        BlockPos targetPos = hit.getBlockPos();
        Direction side = hit.getSide();
        Block targetedBlock = client.world.getBlockState(targetPos).getBlock();
        BlockPos placePos = targetPos.offset(side);

        if (!client.world.getBlockState(placePos).isAir()) {
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
                if (targetedBlock != Blocks.TRIPWIRE || side != Direction.UP) {
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

    private void executePlacement(MinecraftClient client, BlockHitResult hit, Item item) {
        PlayerInventory inv = client.player.getInventory();
        int slot = -1;

        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isOf(item)) {
                slot = i;
                break;
            }
        }

        if (slot == -1) return;

        int previousSlot = inv.selectedSlot;
        inv.selectedSlot = slot;
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
        inv.selectedSlot = previousSlot;
    }
}