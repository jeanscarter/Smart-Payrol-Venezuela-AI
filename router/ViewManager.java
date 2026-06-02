package com.nomina.router;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Sistema de enrutamiento basado en {@link CardLayout}.
 * <p>
 * Gestiona las vistas principales del dashboard y provee navegación
 * por clave con callback de cambio de vista para sincronizar
 * componentes externos (Sidebar, Breadcrumbs, Header).
 * <p>
 * Uso típico:
 * <pre>{@code
 *   ViewManager vm = ViewManager.getInstance();
 *   vm.register("inicio", new InicioPanel());
 *   vm.register("empleados", new EmpleadosPanel());
 *   vm.show("inicio");
 *   frame.add(vm.getContainer());
 * }</pre>
 */
public final class ViewManager {

    private static ViewManager instance;

    private final JPanel container;
    private final CardLayout cardLayout;
    private final Map<String, JComponent> views;
    private String currentViewKey;
    private Consumer<String> onViewChanged;

    private ViewManager() {
        this.cardLayout = new CardLayout();
        this.container = new JPanel(cardLayout);
        this.container.setOpaque(false);
        this.views = new LinkedHashMap<>();
    }

    /**
     * @return Instancia única del ViewManager.
     */
    public static ViewManager getInstance() {
        if (instance == null) {
            instance = new ViewManager();
        }
        return instance;
    }

    /**
     * Registra una vista con una clave única. Si la clave ya existe, la vista anterior
     * es reemplazada.
     *
     * @param key  Clave de identificación (e.g., "inicio", "empleados", "nomina")
     * @param view Componente visual de la vista
     */
    public void register(String key, JComponent view) {
        if (views.containsKey(key)) {
            container.remove(views.get(key));
        }
        views.put(key, view);
        container.add(view, key);
    }

    /**
     * Muestra la vista asociada a la clave, disparando el callback {@code onViewChanged}.
     *
     * @param key Clave de la vista registrada
     * @throws IllegalArgumentException si la clave no está registrada
     */
    public void show(String key) {
        if (!views.containsKey(key)) {
            throw new IllegalArgumentException("Vista no registrada: " + key);
        }
        currentViewKey = key;
        cardLayout.show(container, key);
        if (onViewChanged != null) {
            onViewChanged.accept(key);
        }
    }

    /**
     * @return Panel contenedor de todas las vistas (para montar en el JFrame).
     */
    public JPanel getContainer() {
        return container;
    }

    /**
     * @return Clave de la vista actualmente visible, o {@code null} si ninguna ha sido mostrada.
     */
    public String getCurrentViewKey() {
        return currentViewKey;
    }

    /**
     * @return {@code true} si la clave está registrada en el router.
     */
    public boolean hasView(String key) {
        return views.containsKey(key);
    }

    /**
     * @return Mapa inmutable de vistas registradas (clave → componente).
     */
    public Map<String, JComponent> getViews() {
        return Map.copyOf(views);
    }

    /**
     * Registra un callback que se ejecuta cada vez que la vista activa cambia.
     * Útil para sincronizar Sidebar, Breadcrumbs y Header con la vista actual.
     *
     * @param callback Consumer que recibe la clave de la nueva vista activa
     */
    public void setOnViewChanged(Consumer<String> callback) {
        this.onViewChanged = callback;
    }
}
