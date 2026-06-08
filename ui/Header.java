package com.nomina.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.nomina.theme.ThemeManager;
import com.nomina.service.BcvApiService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/**
 * Header superior del dashboard principal.
 * <p>
 * Layout horizontal:
 * ┌──────────────────────────────────────────────────────────┐
 * │  📍 Breadcrumb        Tasa BCV: Bs. XX.XX  🔄  ☀/🌙     │
 * └──────────────────────────────────────────────────────────┘
 * <ul>
 *   <li>Breadcrumb dinámico sincronizado con ViewManager</li>
 *   <li>Indicador de Tasa BCV actualizable automáticamente</li>
 *   <li>Toggle de tema Light/Dark</li>
 * </ul>
 */
public class Header extends JPanel {

    private final JLabel breadcrumbLabel;
    private final JLabel tasaBcvLabel;
    private final JButton themeToggle;
    private final JButton syncBcvButton;

    // ── Mapa humano de claves de vista ──
    private static String getViewDisplayName(String key) {
        return switch (key) {
            case "inicio"        -> "Inicio";
            case "empleados"     -> "Empleados";
            case "nomina"        -> "Nómina";
            case "reportes"      -> "Reportes";
            case "configuracion" -> "Configuración";
            case "acerca"        -> "Acerca de";
            default              -> key;
        };
    }

    public Header() {
        setLayout(new MigLayout(
                "insets 12 24 12 24", "[]push[]4[]16[]", "[center]"));
        putClientProperty(FlatClientProperties.STYLE,
                "background: $control");

        // ── Breadcrumb ──
        breadcrumbLabel = new JLabel(buildBreadcrumbText("inicio"));
        breadcrumbLabel.putClientProperty(FlatClientProperties.STYLE,
                "font: bold +1");

        // ── Tasa BCV ──
        tasaBcvLabel = new JLabel(buildTasaText(com.nomina.config.ConfigManager.getTasaBcv()));
        tasaBcvLabel.putClientProperty(FlatClientProperties.STYLE,
                "foreground: $Button.default.background; font: bold -1");
        tasaBcvLabel.setToolTipText("Tasa oficial BCV — actualizable automáticamente");

        // Registrar listener para actualización en caliente
        com.nomina.config.ConfigManager.addListener(() -> {
            tasaBcvLabel.setText(buildTasaText(com.nomina.config.ConfigManager.getTasaBcv()));
        });

        // ── Botón Sincronizar BCV ──
        syncBcvButton = new JButton("🔄");
        syncBcvButton.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        syncBcvButton.putClientProperty(FlatClientProperties.STYLE,
                "arc: 10; font: -1");
        syncBcvButton.setToolTipText("Sincronizar Tasa BCV automáticamente");
        syncBcvButton.setFocusable(false);
        syncBcvButton.addActionListener(e -> {
            syncBcvButton.setEnabled(false);
            syncBcvButton.setText("⏳");

            SwingWorker<Double, Void> worker = new SwingWorker<>() {
                @Override
                protected Double doInBackground() throws Exception {
                    return BcvApiService.fetchTasaBcv();
                }

                @Override
                protected void done() {
                    try {
                        double newTasa = get();
                        com.nomina.config.ConfigManager.setTasaBcv(newTasa);
                        com.nomina.config.ConfigManager.save();
                        JOptionPane.showMessageDialog(Header.this,
                                "Tasa BCV sincronizada con éxito: Bs. " + String.format("%,.4f", newTasa),
                                "Sincronización Exitosa",
                                JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        JOptionPane.showMessageDialog(Header.this,
                                "Error al sincronizar la tasa BCV:\n" + cause.getMessage(),
                                "Error de Sincronización",
                                JOptionPane.ERROR_MESSAGE);
                    } finally {
                        syncBcvButton.setEnabled(true);
                        syncBcvButton.setText("🔄");
                    }
                }
            };
            worker.execute();
        });

        // ── Theme Toggle ──
        themeToggle = new JButton(getThemeIcon());
        themeToggle.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        themeToggle.putClientProperty(FlatClientProperties.STYLE,
                "arc: 10; font: +2");
        themeToggle.setToolTipText("Cambiar tema Light/Dark");
        themeToggle.setFocusable(false);
        themeToggle.addActionListener(e -> {
            ThemeManager.toggle();
            themeToggle.setText(getThemeIcon());
        });

        // ── Theme listener para actualizar estado ──
        ThemeManager.addListener(isDark -> {
            themeToggle.setText(getThemeIcon());
            tasaBcvLabel.repaint();
        });

        add(breadcrumbLabel);
        add(tasaBcvLabel);
        add(syncBcvButton);
        add(themeToggle);
    }

    /**
     * Actualiza el breadcrumb al navegar a una nueva vista.
     *
     * @param viewKey Clave de la vista actual del ViewManager
     */
    public void updateBreadcrumb(String viewKey) {
        breadcrumbLabel.setText(buildBreadcrumbText(viewKey));
    }

    /**
     * Actualiza la tasa BCV mostrada en el header.
     *
     * @param tasa Valor de la tasa oficial BCV (VES por 1 USD)
     */
    public void updateTasaBcv(double tasa) {
        tasaBcvLabel.setText("  \uD83C\uDFE6  Tasa BCV: Bs. " + String.format("%,.2f", tasa));
    }

    // ── Builders ───────────────────────────────────────────────────

    private static String buildBreadcrumbText(String viewKey) {
        return "\uD83D\uDCCD  Nómina Inteligente  ›  " + getViewDisplayName(viewKey);
    }

    private static String buildTasaText(double tasa) {
        return "  \uD83C\uDFE6  Tasa BCV: Bs. " + String.format("%,.2f", tasa);
    }

    private static String getThemeIcon() {
        return ThemeManager.isDark() ? "\u2600" : "\uD83C\uDF19"; // ☀ / 🌙
    }
}
