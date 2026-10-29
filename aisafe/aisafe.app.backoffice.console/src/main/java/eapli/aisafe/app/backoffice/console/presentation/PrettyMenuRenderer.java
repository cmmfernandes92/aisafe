package eapli.aisafe.app.backoffice.console.presentation;

import java.util.ArrayList;
import java.util.List;

import eapli.framework.actions.menu.Menu;
import eapli.framework.actions.menu.MenuItem;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class PrettyMenuRenderer extends VerticalMenuRenderer {

    private static final int BOX = 46;
    private final String title;

    public PrettyMenuRenderer(final Menu menu, final MenuItemRenderer itemRenderer, final String title) {
        super(menu, itemRenderer);
        this.title = title;
    }

    @Override
    protected void doRender() {
        final List<MenuItem> main = new ArrayList<>();
        final List<MenuItem> zero = new ArrayList<>();

        for (final MenuItem item : menu.itens()) {
            item.option().ifPresent(opt -> {
                if (opt == 0) zero.add(item);
                else main.add(item);
            });
        }

        System.out.println();
        top();
        row(stripArrow(title).toUpperCase());
        mid();
        for (final MenuItem item : main) {
            row(item.option().get() + ".  " + stripArrow(item.text()));
        }
        if (!zero.isEmpty()) {
            mid();
            for (final MenuItem item : zero) {
                row(item.option().get() + ".  " + stripArrow(item.text()));
            }
        }
        bot();
    }

    @Override
    protected boolean doRenderSubmenu(final Menu sub) {
        System.out.println();
        new PrettyMenuRenderer(sub, itemRenderer, sub.title()).render();
        return false;
    }

    private String stripArrow(final String text) {
        final String t = text.stripTrailing();
        return t.endsWith(">") ? t.substring(0, t.length() - 1).stripTrailing() : t;
    }

    private void top() { System.out.println("  ┌" + "─".repeat(BOX) + "┐"); }
    private void mid() { System.out.println("  ├" + "─".repeat(BOX) + "┤"); }
    private void bot() { System.out.println("  └" + "─".repeat(BOX) + "┘"); }

    private void row(final String content) {
        System.out.printf("  │  %-" + (BOX - 2) + "s│%n", content);
    }
}
