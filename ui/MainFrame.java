package com.nomina.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.nomina.router.ViewManager;
import com.nomina.ui.views.*;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Frame principal de la aplicación Nómina Inteligente.
 * <p>
 * Implementa el layout Shell (Fase 2):
 * - Sidebar lateral izquierda (fondo {@code @control}) con navegación.
 * - Header superior derecho (Breadcrumbs + Tasa BCV + Theme Toggle).
 * - Área de contenido centralizada (fondo {@code @background}) administrada por {@link ViewManager}.
 */
public class MainFrame extends JFrame {

    private static final int MIN_WIDTH  = 1280;
    private static final int MIN_HEIGHT = 720;

    private Sidebar sidebar;
    private Header header;

    public MainFrame() {
        setTitle("Nómina Inteligente — Sistema de Nómina Multi-Divisa");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setSize(1440, 850);
        setLocationRelativeTo(null);

        initComponents();
    }

    private void initComponents() {
        // --- ROOT CONTAINER WITH 2-COLUMN LAYOUT ---
        // Column 1: Sidebar (width 260px)
        // Column 2: Right panel (Header + Content Area) (takes remaining space)
        JPanel root = new JPanel(new MigLayout(
                "fill, insets 0, gap 0",
                "[260!, fill][grow, fill]",
                "[grow, fill]"
        ));
        root.putClientProperty(FlatClientProperties.STYLE, "background: $background");

        // --- INSTANCIAR COMPONENTES DEL SHELL ---
        sidebar = new Sidebar();
        header = new Header();

        // --- REGISTRAR VISTAS EN EL ENRUTADOR ---
        ViewManager viewManager = ViewManager.getInstance();
        viewManager.register("inicio", new InicioPanel());
        viewManager.register("empleados", new EmpleadosPanel());
        viewManager.register("nomina", new NominaPanel());
        viewManager.register("reportes", new ReportesPanel());
        viewManager.register("configuracion", new ConfiguracionPanel());
        viewManager.register("acerca", new AcercaPanel());

        // Vista inicial por defecto
        viewManager.show("inicio");

        // --- PANEL DERECHO (HEADER + VIEW CONTAINER) ---
        JPanel rightPanel = new JPanel(new MigLayout(
                "fill, insets 0, gap 0, wrap",
                "[grow, fill]",
                "[]0[grow, fill]"
        ));
        rightPanel.setOpaque(false);

        // Contenedor del ViewManager con padding y fondo correcto
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setOpaque(false);
        contentWrapper.add(viewManager.getContainer(), BorderLayout.CENTER);

        rightPanel.add(header, "growx");
        rightPanel.add(contentWrapper, "grow, push");

        // --- AGREGAR AL CONTENEDOR PRINCIPAL ---
        root.add(sidebar, "growy");
        root.add(rightPanel, "grow");

        setContentPane(root);

        // --- ESCUCHAR EVENTOS DE NAVEGACIÓN ---
        // Sincroniza la barra de navegación del sidebar y los breadcrumbs del header
        viewManager.setOnViewChanged(key -> {
            sidebar.setActive(key);
            header.updateBreadcrumb(key);
        });

        // Configuración inicial de la Tasa BCV
        header.updateTasaBcv(com.nomina.config.ConfigManager.getTasaBcv());
    }
}
