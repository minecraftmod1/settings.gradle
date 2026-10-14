package com.facehud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class FaceConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("facehud.json");

	public float x = 0.02f;
	public float y = 0.03f;
	public float width = 0.10f;
	public boolean visible = true;

	private static FaceConfig instance = new FaceConfig();

	public static FaceConfig get() {
		return instance;
	}

	public static void load() {
		if (!Files.exists(FILE)) {
			instance = new FaceConfig();
			instance.save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(FILE)) {
			FaceConfig loaded = GSON.fromJson(reader, FaceConfig.class);
			instance = loaded != null ? loaded : new FaceConfig();
		} catch (Exception e) {
			System.err.println("[facehud] Could not read " + FILE + ", using defaults: " + e);
			instance = new FaceConfig();
		}
		instance.sanitize();
	}

	public void save() {
		sanitize();
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			System.err.println("[facehud] Could not save " + FILE + ": " + e);
		}
	}

	private void sanitize() {
		if (!(width > 0.005f) || width > 1f) width = 0.10f;
		if (!(x >= 0f)) x = 0f;
		if (!(y >= 0f)) y = 0f;
		if (x > 1f) x = 1f;
		if (y > 1f) y = 1f;
	}

	public static final int MIN_WIDTH_PX = 16;

	public static int pxWidth(float widthFraction, int screenWidth) {
		return Math.max(MIN_WIDTH_PX, Math.round(widthFraction * screenWidth));
	}

	public static int pxHeight(int widthPx) {
		return Math.max(1, Math.round(widthPx * (float) FaceHudClient.texH / FaceHudClient.texW));
	}
}
