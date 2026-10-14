package com.facehud;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.io.DataInputStream;
import java.io.InputStream;

public class FaceHudClient implements ClientModInitializer {
	public static final String MOD_ID = "facehud";

	public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/face.png");

	public static int texW = 32;
	public static int texH = 32;

	private static KeyMapping openEditorKey;

	@Override
	public void onInitializeClient() {
		readTextureSize();
		FaceConfig.load();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		openEditorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.facehud.open_editor",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_F8,
				category
		));

		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(MOD_ID, "face"),
				FaceHudClient::renderHud
		);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openEditorKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new PlacementScreen());
				}
			}
		});
	}

	private static void renderHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen instanceof PlacementScreen) {
			return;
		}
		FaceConfig cfg = FaceConfig.get();
		if (!cfg.visible) {
			return;
		}

		int screenW = mc.getWindow().getGuiScaledWidth();
		int screenH = mc.getWindow().getGuiScaledHeight();
		int w = FaceConfig.pxWidth(cfg.width, screenW);
		int h = FaceConfig.pxHeight(w);
		int x = Math.round(cfg.x * screenW);
		int y = Math.round(cfg.y * screenH);

		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0f, 0f, w, h, texW, texH, texW, texH);
	}

	private static void readTextureSize() {
		try (InputStream in = FaceHudClient.class.getResourceAsStream("/assets/" + MOD_ID + "/textures/gui/face.png")) {
			if (in == null) return;
			DataInputStream data = new DataInputStream(in);
			data.skipNBytes(16);
			int w = data.readInt();
			int h = data.readInt();
			if (w > 0 && h > 0) {
				texW = w;
				texH = h;
			}
		} catch (Exception e) {
			System.err.println("[facehud] Could not read face.png size, assuming 32x32: " + e);
		}
	}
}
