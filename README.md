# 💸 Nómina Inteligente - Sistema Multi-Moneda (USD/VES) 🇻🇪
[![Java Version](https://img.shields.io/badge/Java-21%2B-blue?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![UI Framework](https://img.shields.io/badge/UI-FlatLaf%203.5.4-emerald?logo=java&logoColor=white)](https://www.formdev.com/flatlaf/)
[![Layout](https://img.shields.io/badge/Layout-MigLayout%2011.4.2-orange)](http://www.miglayout.com/)
[![PDF Engine](https://img.shields.io/badge/PDF-OpenPDF%202.0.2-red?logo=adobe-acrobat-reader&logoColor=white)](https://github.com/LibrePDF/OpenPDF)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
**Nómina Inteligente** es una aplicación de escritorio moderna desarrollada en Java Swing para la gestión integral y el procesamiento de nóminas en el contexto socioeconómico venezolano. La aplicación está diseñada bajo el estándar de **facturación y cálculo bi-monetario** (Dólares USD / Bolívares VES) y cumple estrictamente con las regulaciones laborales vigentes de la **LOTTT** y los distintos entes gubernamentales (IVSS, BANAVIH, SENIAT, MinPPTRASS).
---
## ✨ Características Principales
### 🖥️ Interfaz de Usuario y Experiencia (UI/UX)
*   **Tema FlatLaf Moderno:** Totalmente integrado con soporte nativo para **Modo Claro (Light Mode)** y **Modo Oscuro (Dark Mode)** en tiempo de ejecución.
*   **Diseño Responsivo con MigLayout:** Formularios y tablas adaptables que garantizan una distribución óptima de los componentes gráficos.
*   **Estilos Limpios y Modernos:** Uso extenso de bordes redondeados, tipografías del sistema optimizadas y micro-interacciones (hovers y transiciones).
### 🧮 Motor de Cálculos y Regulación LOTTT
*   **Conversión en Tiempo Real (USD / VES):** Utiliza la tasa oficial del Banco Central de Venezuela (BCV) para calcular y mostrar salarios en ambas monedas de forma simultánea.
*   **Cálculo de Variables del Período:**
    *   **Horas Extras (HE):** Recargo legal del 50% sobre la hora ordinaria.
    *   **Horas Nocturnas (HN):** Recargo legal del 30% sobre el salario base de la jornada.
    *   **Días Feriados Trabajados:** Pago doble de la jornada diaria (recargo del 50% sobre el salario ordinario de acuerdo al Art. 142/184 LOTTT).
    *   **Deducciones por Inasistencias:** Descuento directo por días no laborados.
    *   **Adelantos de Quincena:** Deducciones de adelantos entregados tanto en Bolívares (VES) como en Dólares (USD).
*   **Cesta Ticket Socialista:** Flexibilidad para configurar montos quincenales/mensuales estandarizados o anclados a la tasa de cambio de manera fija (ej. $20 quincenales / $40 mensuales).
### 📋 Obligaciones de Ley y Reportes Oficiales
El módulo de **Reportes** permite exportar en formatos planos (.TXT) o visualizar la información exigida por los entes del Estado:
1.  **FAOV Banavih:** Consolidación mensual de aportes al Fondo de Ahorro Obligatorio para la Vivienda (1% retención trabajador, 2% aporte patrono).
2.  **IVSS - Forma 14-02:** Reporte de registro y control de trabajadores ante el Instituto Venezolano de los Seguros Sociales.
3.  **IVSS - Providencia 003:** Detalle de cotizaciones activas desglosadas (4% trabajador / 9% patrono).
4.  **Prestaciones Sociales Detalladas (Art. 142/143 LOTTT):** Cálculo acumulado del fideicomiso de garantía de prestaciones (15 días por trimestre) y días de antigüedad adicionales (2 días por año a partir del 2do año, acumulativos hasta 30 días), incluyendo el cálculo mensual de **intereses sobre prestaciones** a la tasa promedio activa del BCV.
5.  **Contribución Especial de Pensiones (SENIAT):** Declaración del aporte patronal especial para la protección de pensiones sobre la base del salario integral del trabajador.
6.  **ARCV (ISLR):** Comprobante anual de retenciones de Impuesto sobre la Renta estimado para personas naturales residentes en base al valor de la Unidad Tributaria (anclada al salario mínimo/tasa).
7.  **Declaración Trimestral MinPPTRASS:** Archivo de texto plano con la declaración obligatoria trimestral de empleo, horas extras y salarios ante el Ministerio del Trabajo.
### 🔗 Integración Contable y PDFs
*   **Asiento Contable ERP:** Generación automática del asiento de nómina agrupado por departamentos (Tecnología, Administración, Operaciones, etc.), listo para importar en sistemas de contabilidad corporativos (SAP, Profit Plus, Saint, etc.). Cuadre perfecto de cuentas de Gastos (Debe) contra Pasivos y Bancos (Haber).
*   **Recibos de Pago PDF:** Emisión individual automatizada de recibos de pago en PDF de alta calidad, listos para imprimir y firmar, con desglose detallado de todos los conceptos y firma del trabajador.
---
## 📂 Estructura del Proyecto
El código está estructurado bajo las mejores prácticas de arquitectura de software, separando la lógica de presentación (UI) de las reglas de negocio (Service) y persistencia (Repository):
```
com.nomina
│
├── Main.java                        # Punto de entrada de la aplicación
│
├── config
│   └── ConfigManager.java           # Gestor de configuraciones y propiedades persistentes
│
├── model
│   ├── Empleado.java                # Modelo del Trabajador (Salario, cargo, fecha de ingreso)
│   └── ReciboNomina.java            # Modelo del cálculo detallado del recibo quincenal/mensual
│
├── repository
│   ├── EmpleadoRepository.java      # CRUD y Persistencia local (CSV) de empleados
│   └── NominaRepository.java        # CRUD y Persistencia local (CSV) del histórico de nóminas
│
├── service
│   ├── ContabilidadService.java     # Generación de asientos contables y reportes MinPPTRASS
│   ├── CsvImporter.java             # Utilidad para importación masiva de empleados
│   ├── LegalReportService.java      # Cálculos de FAOV, IVSS, Prestaciones LOTTT, ARCV y SENIAT
│   ├── PayrollService.java          # Lógica central del cálculo de nómina y conversiones
│   └── PdfService.java              # Renderizado de recibos de pago individuales con OpenPDF
│
├── theme
│   └── ThemeManager.java            # Configuración dinámica y personalización del Look & Feel
│
└── ui                               # Capa de Interfaz Gráfica (Swing)
    ├── MainFrame.java               # Frame principal y barra de menú
    ├── Header.java                  # Panel superior con reloj y datos de la empresa
    ├── Sidebar.java                 # Menú de navegación lateral reactivo
    └── views
        ├── InicioPanel.java         # Dashboard inicial con KPI globales
        ├── EmpleadosPanel.java      # Gestión del directorio de personal (Formularios)
        ├── NominaPanel.java         # Procesamiento, variables de periodo y simulación quincenal
        ├── ReportesPanel.java       # Panel centralizado de exportaciones legales
        ├── ConfiguracionPanel.java  # Ajustes de tasas, RIF, porcentajes y variables
        └── AcercaPanel.java         # Créditos e información de versión
```
---
## 🛠️ Requisitos del Sistema
*   **Java Development Kit (JDK):** Versión 21 o superior.
*   **Apache Maven:** Versión 3.8 o superior (para la gestión de dependencias y construcción).
---
## 🚀 Instalación y Ejecución
Sigue estos sencillos pasos para compilar y ejecutar el sistema localmente:
### 1. Clonar el repositorio
```bash
git clone https://github.com/jeanscarter/ai-payroll-venezuela.git
cd ai-payroll-venezuela
```
### 2. Compilar el proyecto con Maven
```bash
mvn clean compile
```
### 3. Ejecutar la aplicación
```bash
mvn exec:java
```
### 4. Construir el empaquetado ejecutable (.JAR)
```bash
mvn clean package
```
El archivo JAR autoejecutable se generará en la carpeta `target/nomina-inteligente-1.0.0-SNAPSHOT.jar`.
---
## ⚙️ Persistencia de Datos Locales
El sistema utiliza archivos locales planos para garantizar la portabilidad inmediata sin necesidad de configurar un motor de bases de datos relacionales:
*   `config.properties`: Parámetros globales configurables (Tasas de cambio, porcentajes de retención, RIF, etc.).
*   `empleados.csv`: Base de datos de los empleados registrados.
*   `nominas_procesadas.csv`: Historial y archivo de las nóminas aprobadas para auditorías.
---
## 📄 Licencia
Este proyecto está bajo la licencia **MIT**. Consulte el archivo [LICENSE](LICENSE) para obtener más detalles.
