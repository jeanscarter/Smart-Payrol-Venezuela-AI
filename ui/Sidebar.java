package com.nomina.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.nomina.router.ViewManager;
import com.nomina.theme.ThemeManager;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Sidebar de navegación principal con fondo {@code @control}.
 * <p>
 * Estructura:
 * <pre>
 * ┌──────────────────┐
 * │  Logo + Nombre   │
 * │  ─────────────── │
 * │  ▸ Inicio        │
 * │  ▸ Empleados     │
 * │  ▸ Nómina        │
 * │  ▸ Configuración │
 * │                  │
 * │  ─────────────── │
 * │  ▸ Acerca de     │
 * └──────────────────┘
 * </pre>
 * Cada item detecta hover/press/active con micro-animaciones
 * de color. Se integra con {@link ViewManager} para sincronizar
 * la vista activa.
 */
public class Sidebar extends JPanel {

    private static final int SIDEBAR_WIDTH = 260;
    private final List<NavItem> navItems = new ArrayList<>();
    private String activeView = "inicio";

    /**
     * Definición de un ítem de navegación.
     *
     * @param icon    Carácter Unicode del ícono
     * @param label   Texto visible
     * @param viewKey Clave del ViewManager para la navegación
     */
    public record NavItemDef(String icon, String label, String viewKey) {
    }

    // ── Ítems de navegación principal ──
    private static final NavItemDef[] MAIN_ITEMS = {
            new NavItemDef("\uD83C\uDFE0", "Inicio",        "inicio"),        // 🏠
            new NavItemDef("\uD83D\uDC65", "Empleados",     "empleados"),     // 👥
            new NavItemDef("\uD83D\uDCB0", "Nómina",        "nomina"),        // 💰
            new NavItemDef("\uD83D\uDC8A", "Mercancía",     "mercancia"),     // 💊
            new NavItemDef("\uD83D\uDCC8", "Reportes",      "reportes"),      // 📈
            new NavItemDef("\u2699",       "Configuración", "configuracion"), // ⚙
    };

    // ── Ítems secundarios ──
    private static final NavItemDef[] FOOTER_ITEMS = {
            new NavItemDef("\u2139",  "Acerca de", "acerca"),  // ℹ
    };

    public Sidebar() {
        setLayout(new MigLayout(
                "fill, wrap, insets 0", "[grow, fill]", "[]12[grow, fill][]"));
        putClientProperty(FlatClientProperties.STYLE,
                "background: $control");
        setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));

        add(createLogoPanel(),  "growx");
        add(createNavPanel(),   "growx, growy, pushy");
        add(createFooterPanel(),"growx");

        // ── Sincronizar con ViewManager ──
        ViewManager.getInstance().setOnViewChanged(key -> setActive(key));

        // ── Theme-awareness ──
        ThemeManager.addListener(isDark -> repaint());
    }

    /**
     * Establece el ítem activo por su viewKey y repinta.
     */
    public void setActive(String viewKey) {
        this.activeView = viewKey;
        for (NavItem item : navItems) {
            item.updateState();
        }
    }

    // ── Logo Panel ─────────────────────────────────────────────────

    private JPanel createLogoPanel() {
        JPanel panel = new JPanel(new MigLayout(
                "insets 24 20 20 20, gapy 0", "[center, grow]", "[]4[]"));
        panel.setOpaque(false);

        JLabel logoIcon = new JLabel("\uD83D\uDCCA"); // 📊
        logoIcon.setFont(logoIcon.getFont().deriveFont(32f));

        JLabel logoText = new JLabel("Nómina Inteligente");
        logoText.putClientProperty(FlatClientProperties.STYLE,
                "font: bold +4");

        JLabel versionLabel = new JLabel("Multi-Divisa v1.0");
        versionLabel.putClientProperty(FlatClientProperties.STYLE,
                "foreground: $Label.disabledForeground; font: -2");

        panel.add(logoIcon, "wrap");
        panel.add(logoText, "wrap");
        panel.add(versionLabel);

        return panel;
    }

    // ── Navigation Panel ───────────────────────────────────────────

    private JPanel createNavPanel() {
        JPanel panel = new JPanel(new MigLayout(
                "wrap, insets 12 8 12 8, gapy 2", "[grow, fill]", ""));
        panel.setOpaque(false);

        // Separador superior
        panel.add(createSeparator(), "growx, gapbottom 8");

        for (NavItemDef def : MAIN_ITEMS) {
            NavItem item = new NavItem(def);
            navItems.add(item);
            panel.add(item, "growx, h 42!");
        }

        return panel;
    }

    // ── Footer Panel ───────────────────────────────────────────────

    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new MigLayout(
                "wrap, insets 8 8 16 8, gapy 2", "[grow, fill]", ""));
        panel.setOpaque(false);

        panel.add(createSeparator(), "growx, gapbottom 8");

        for (NavItemDef def : FOOTER_ITEMS) {
            NavItem item = new NavItem(def);
            navItems.add(item);
            panel.add(item, "growx, h 42!");
        }

        return panel;
    }

    // ── Separator ──────────────────────────────────────────────────

    private JSeparator createSeparator() {
        JSeparator sep = new JSeparator();
        sep.putClientProperty(FlatClientProperties.STYLE,
                "height: 1");
        return sep;
    }

    // ═══════════════════════════════════════════════════════════════
    // ── NavItem: botón de navegación con hover/active/press ──
    // ═══════════════════════════════════════════════════════════════

    /**
     * Componente visual individual para cada ítem del sidebar.
     * Renderizado custom via {@code paintComponent} para control
     * total de colores, redondez y micro-animaciones de hover.
     */
    private class NavItem extends JPanel {

        private final NavItemDef def;
        private boolean hovered = false;
        private boolean pressed = false;

        NavItem(NavItemDef def) {
            this.def = def;
            setLayout(new MigLayout(
                    "insets 0 16 0 16, gap 12", "[][]push", "[center]"));
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel iconLabel = new JLabel(def.icon());
            iconLabel.setFont(iconLabel.getFont().deriveFont(16f));

            JLabel textLabel = new JLabel(def.label());
            textLabel.putClientProperty(FlatClientProperties.STYLE,
                    "font: $semibold +1");

            add(iconLabel);
            add(textLabel);

            // ── Mouse events ──
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    pressed = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    pressed = true;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    pressed = false;
                    if (hovered) {
                        activeView = def.viewKey();
                        for (NavItem ni : navItems) {
                            ni.updateState();
                        }
                        ViewManager.getInstance().show(def.viewKey());
                    }
                    repaint();
                }
            });
        }

        void updateState() {
            repaint();
        }

        boolean isActive() {
            return def.viewKey().equals(activeView);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int arc = 12;
            int w = getWidth();
            int h = getHeight();

            if (isActive()) {
                // Fondo del ítem activo: accent translúcido
                g2.setColor(UIManager.getColor("Component.focusColor"));
                g2.setComposite(AlphaComposite.SrcOver.derive(0.15f));
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                g2.setComposite(AlphaComposite.SrcOver);

                // Barra lateral izquierda indicadora
                g2.setColor(UIManager.getColor("Component.focusColor"));
                g2.fillRoundRect(0, 6, 3, h - 12, 4, 4);

            } else if (pressed) {
                g2.setColor(UIManager.getColor("Component.focusColor"));
                g2.setComposite(AlphaComposite.SrcOver.derive(0.10f));
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                g2.setComposite(AlphaComposite.SrcOver);

            } else if (hovered) {
                Color fg = UIManager.getColor("@foreground");
                if (fg == null) fg = getForeground();
                g2.setColor(fg);
                g2.setComposite(AlphaComposite.SrcOver.derive(0.06f));
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                g2.setComposite(AlphaComposite.SrcOver);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
