package me.shimmy.tlntd;

import net.minecraft.client.gui.DrawContext;

public interface IMenuSection {
    void render(DrawContext ctx, int x, int y, int w, int mx, int my, int accent, float delta);
    boolean mouseClicked(double mx, double my, int btn, int x, int y, int w);
    void updateDragging(int mx, int startX);
    default boolean onKeyPressed(int key, int scan, int mods) { return false; }
    default boolean onCharTyped(char chr, int mods)           { return false; }
    default void mouseReleased()                               {}
    /** Estimated total content height in pixels. Used for outer scrollbar. */
    default int preferredHeight(int w)                         { return 300; }
}
