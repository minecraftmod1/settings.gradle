package com.facehud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class PlacementScreen extends Screen {
	private static final int HANDLE = 8;

	private enum Drag { NONE, MOVE, RESIZE }

	private float fx;
	private float fy;
	private float fw;

	private Drag drag = Drag.NONE;
	private double grabDx;
	private double grabDy;

	private final List<Button> toolbar = new ArrayList<>();

	public PlacementScreen() {
		super(Component.translatable("screen.facehud.title"));
		FaceConfig cfg = FaceConfig.get();
		this.fx = cfg.x;
		this.fy = cfg.y;
		this.fw = cfg.width;
	}

	@Override
	protected void init() {
		toolbar.clear();
		clampToScreen();

		int x0 = this.width / 2 - 122;
		int y = this.height - 28;
		toolbar.add(this.addRenderableWidget(
				Button.builder(Component.literal("-"), b -> scaleBy(1 / 1.1)).bounds(x0, y, 20, 20).build()));
		toolbar.add(this.addRenderableWidget(
				Button.builder(Component.literal("+"), b -> scaleBy(1.1)).bounds(x0 + 24, y, 20, 20).build()));
		toolbar.add(this.addRenderableWidget(
				Button.builder(Component.literal("Reset"), b -> reset()).bounds(x0 + 56, y, 60, 20).build()));
		toolbar.add(this.addRenderableWidget(
				Button.builder(Component.literal("Cancel"), b -> this.onClose()).bounds(x0 + 120, y, 60, 20).build()));
		toolbar.add(this.addRenderableWidget(
				Button.builder(Component.literal("Save"), b -> save()).bounds(x0 + 184, y, 60, 20).build()));
	}

	private void save() {
		FaceConfig cfg = FaceConfig.get();
		cfg.x = fx;
		cfg.y = fy;
		cfg.width = fw;
		cfg.save();
		this.onClose();
	}

	private void reset() {
		FaceConfig defaults = new FaceConfig();
		fx = defaults.x;
		fy = defaults.y;
		fw = defaults.width;
		clampToScreen();
	}

	private double aspect() {
		return (double) FaceHudClient.texH / FaceHudClient.texW;
	}

	private int pxW() {
		return FaceConfig.pxWidth(fw, this.width);
	}

	private int pxH() {
		return FaceConfig.pxHeight(pxW());
	}

	private int pxX() {
		return Math.round(fx * this.width);
	}

	private int pxY() {
		return Math.round(fy * this.height);
	}

	private void setPosPx(double xpx, double ypx) {
		int w = pxW();
		int h = pxH();
		xpx = Math.max(0, Math.min(xpx, this.width - w));
		ypx = Math.max(0, Math.min(ypx, this.height - h));
		fx = (float) (xpx / this.width);
		fy = (float) (ypx / this.height);
	}

	private void setWidthPx(double wpx) {
		double x = fx * this.width;
		double y = fy * this.height;
		double maxW = Math.min(this.width - x, (this.height - y) / aspect());
		wpx = Math.max(FaceConfig.MIN_WIDTH_PX, Math.min(wpx, maxW));
		fw = (float) (wpx / this.width);
	}

	private void clampToScreen() {
		setPosPx(pxX(), pxY());
		setWidthPx(pxW());
	}

	private void scaleBy(double factor) {
		int oldW = pxW();
		double newW = oldW * factor;
		if (Math.abs(newW - oldW) < 1) {
			newW = oldW + (factor > 1 ? 1 : -1);
		}
		double newH = newW * aspect();
		double cx = pxX() + oldW / 2.0;
		double cy = pxY() + pxH() / 2.0;
		fx = (float) (Math.max(0, cx - newW / 2) / this.width);
		fy = (float) (Math.max(0, cy - newH / 2) / this.height);
		setWidthPx(newW);
	}

	private static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		int x = pxX();
		int y = pxY();
		int w = pxW();
		int h = pxH();

		boolean imageInBottomHalf = (y + h / 2) > this.height / 2;
		int toolbarY = imageInBottomHalf ? 22 : this.height - 28;
		for (Button b : toolbar) {
			b.setY(toolbarY);
		}

		graphics.blit(RenderPipelines.GUI_TEXTURED, FaceHudClient.TEXTURE, x, y, 0f, 0f, w, h,
				FaceHudClient.texW, FaceHudClient.texH, FaceHudClient.texW, FaceHudClient.texH);

		boolean active = drag != Drag.NONE || inside(mouseX, mouseY, x, y, w, h);
		graphics.renderOutline(x - 1, y - 1, w + 2, h + 2, active ? 0xFFFFFF55 : 0xFFFFFFFF);

		graphics.fill(x + w - HANDLE, y + h - HANDLE, x + w, y + h, 0xE0FFFF55);

		String hint = "Drag to move  |  drag the yellow corner or scroll to resize";
		int hintY = imageInBottomHalf ? 8 : this.height - 42;
		graphics.drawString(this.font, hint, (this.width - this.font.width(hint)) / 2, hintY, 0xFFFFFFFF, true);

		super.render(graphics, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		if (event.button() == 0) {
			double mx = event.x();
			double my = event.y();
			int x = pxX();
			int y = pxY();
			int w = pxW();
			int h = pxH();

			if (inside(mx, my, x + w - HANDLE - 2, y + h - HANDLE - 2, HANDLE + 4, HANDLE + 4)) {
				drag = Drag.RESIZE;
				return true;
			}
			if (inside(mx, my, x, y, w, h)) {
				drag = Drag.MOVE;
				grabDx = mx - x;
				grabDy = my - y;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (event.button() == 0 && drag != Drag.NONE) {
			if (drag == Drag.MOVE) {
				setPosPx(event.x() - grabDx, event.y() - grabDy);
			} else {
				double fromX = event.x() - pxX();
				double fromY = (event.y() - pxY()) / aspect();
				setWidthPx((fromX + fromY) / 2.0);
			}
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() == 0 && drag != Drag.NONE) {
			drag = Drag.NONE;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0) {
			scaleBy(scrollY > 0 ? 1.08 : 1 / 1.08);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}
}
