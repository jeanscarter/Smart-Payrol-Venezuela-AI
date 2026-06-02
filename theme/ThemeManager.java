package com.nomina.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestión centralizada del tema visual de Nómina Inteligente.
 * <p>
 * Administra las 5 propiedades semánticas FlatLaf obligatorias:
 * <ul>
 *   <li>{@code @background}  — Fondo principal, contenedores vacíos</li>
 *   <li>{@code @control}     — Cards, paneles internos, campos de texto, tablas</li>
 *   <li>{@code @accentColor} — Botones primarios, foco, elementos activos</li>
 *   <li>{@code Button.default.background} — Acciones secundarias, botones estándar</li>
 *   <li>{@code @foreground}  — Texto principal, íconos activos, headers de tabla</li>
 * </ul>
 * Provee toggle dinámico Light/Dark con notificación a listeners registrados.
 */
public final class ThemeManager {

    private static boolean darkMode = true;
    private static final List<ThemeChangeListener> listeners = new ArrayList<>();

    /**
     * Listener funcional para reaccionar a cambios de tema en tiempo de ejecución.
     */
    @FunctionalInterface
    public interface ThemeChangeListener {
        void onThemeChanged(boolean isDark);
    }

    private ThemeManager() {
    }

    /**
     * Inicializa el sistema de temas. Debe invocarse ANTES de crear cualquier componente Swing.
     */
    public static void init() {
        applyTheme(darkMode);
    }

    /**
     * Alterna entre Light y Dark mode, actualizando toda la UI en caliente.
     */
    public static void toggle() {
        darkMode = !darkMode;
        applyTheme(darkMode);
        refreshAllWindows();
        fireThemeChanged();
    }

    /**
     * @return {@code true} si el modo activo es Dark.
     */
    public static boolean isDark() {
        return darkMode;
    }

    /**
     * Registra un listener que será notificado cada vez que el tema cambie.
     */
    public static void addListener(ThemeChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Remueve un listener previamente registrado.
     */
    public static void removeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }

    // ── Theme Application ──────────────────────────────────────────

    private static void applyTheme(boolean dark) {
        Map<String, String> palette = dark ? buildDarkPalette() : buildLightPalette();
        FlatLaf.setGlobalExtraDefaults(palette);
        try {
            if (dark) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
        } catch (UnsupportedLookAndFeelException e) {
            throw new RuntimeException("Error inicializando tema FlatLaf", e);
        }
    }

    // ── Dark Palette ───────────────────────────────────────────────

    private static Map<String, String> buildDarkPalette() {
        Map<String, String> p = new HashMap<>();

        // ── 5 Semantic Tokens ──
        p.put("@background",              "#0F1923");
        p.put("@control",                  "#1A2332");
        p.put("@accentColor",              "#3B82F6");
        p.put("Button.default.background", "#10B981");
        p.put("@foreground",               "#F1F5F9");

        // ── Shape & Form ──
        p.put("Component.arc",        "12");
        p.put("Button.arc",           "12");
        p.put("TextComponent.arc",    "8");
        p.put("CheckBox.arc",         "4");
        p.put("Component.focusWidth", "2");

        // ── Scrollbar ──
        p.put("ScrollBar.width",       "10");
        p.put("ScrollBar.thumbArc",    "999");
        p.put("ScrollBar.trackArc",    "999");
        p.put("ScrollBar.thumbInsets", "2,2,2,2");

        // ── Table ──
        p.put("Table.showHorizontalLines", "true");
        p.put("Table.showVerticalLines",   "false");

        // ── Title Pane ──
        p.put("TitlePane.unifiedBackground", "true");

        // ── Button Polish ──
        p.put("Button.default.foreground",        "#FFFFFF");
        p.put("Button.default.focusedBackground", "#0EA572");

        return p;
    }

    // ── Light Palette ──────────────────────────────────────────────

    private static Map<String, String> buildLightPalette() {
        Map<String, String> p = new HashMap<>();

        // ── 5 Semantic Tokens ──
        p.put("@background",              "#F1F5F9");
        p.put("@control",                  "#FFFFFF");
        p.put("@accentColor",              "#2563EB");
        p.put("Button.default.background", "#059669");
        p.put("@foreground",               "#0F172A");

        // ── Shape & Form ──
        p.put("Component.arc",        "12");
        p.put("Button.arc",           "12");
        p.put("TextComponent.arc",    "8");
        p.put("CheckBox.arc",         "4");
        p.put("Component.focusWidth", "2");

        // ── Scrollbar ──
        p.put("ScrollBar.width",       "10");
        p.put("ScrollBar.thumbArc",    "999");
        p.put("ScrollBar.trackArc",    "999");
        p.put("ScrollBar.thumbInsets", "2,2,2,2");

        // ── Table ──
        p.put("Table.showHorizontalLines", "true");
        p.put("Table.showVerticalLines",   "false");

        // ── Title Pane ──
        p.put("TitlePane.unifiedBackground", "true");

        // ── Button Polish ──
        p.put("Button.default.foreground",        "#FFFFFF");
        p.put("Button.default.focusedBackground", "#047857");

        return p;
    }

    // ── Window Refresh ─────────────────────────────────────────────

    private static void refreshAllWindows() {
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            window.repaint();
        }
    }

    private static void fireThemeChanged() {
        for (ThemeChangeListener listener : listeners) {
            listener.onThemeChanged(darkMode);
        }
    }
}
