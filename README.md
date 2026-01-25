# Crazy Labyrinth Client

Dieses Projekt ist der JavaFX-basierte Client für das Spiel "Crazy Labyrinth". Es nutzt Spring Boot für die Infrastruktur und JavaFX für die GUI.

## Voraussetzungen

Stellen Sie sicher, dass die folgenden Tools installiert sind:
* **Java SDK 25** (oder höher)
* **Maven 3.9+**
* **Node.js & npm** (für die Model-Generierung)
* **Git**

---

## 1. Shared Models vorbereiten

Der Client benötigt die Modelle aus dem Interface-Repository. Diese müssen generiert, als JAR verpackt und lokal installiert werden.

### Modelle generieren
1. Klonen Sie das Interface-Repository:
   ```bash
   git clone https://github.com/brackettz/labyrinth-interface.git
   cd labyrinth-interface
   ```
2. Installieren Sie die Abhängigkeiten:
   ```bash
   npm install
   ```
3. Generieren Sie die Java-Modelle:
   ```bash
   node models/generate-async-models.js
   ```
   Die generierten Dateien befinden sich nun unter `models/async`.

### JAR bauen & lokal installieren
Falls das Interface-Repo kein eigenes Maven-Build-Script für die Java-Modelle hat, verpacken Sie die generierten Klassen in ein JAR-File. Sobald Sie das JAR (z.B. `shared-model-1.0.0.jar`) haben, installieren Sie es mit folgendem Befehl in Ihr lokales Maven-Repository (`.m2`):
Die Version entspricht den Release Tag im InterFace Repo

## 2. Bauen und Ausführen

### Ausführbares JAR erstellen
Um ein fertiges JAR-File für die Verteilung zu erstellen, führen Sie aus: mvn clean package

Das fertige JAR befindet sich danach im Ordner `target/<artifactId>-<buildVersion>.jar`.


#### Konfiguration und Overrides

Die Anwendung nutzt Standardwerte aus den internen Properties. Sie können diese jedoch leicht überschreiben, ohne das JAR neu bauen zu müssen.

Die Anwendung sucht beim Start an folgenden Orten nach einer `application.properties` (in dieser Reihenfolge):
1.  Im Unterordner `/config` relativ zum JAR-File.
2.  Direkt im selben Ordner wie das JAR-File.
3.  Intern im JAR (Standardwerte).


### Anwendung starten
1. Sie können die Anwendung direkt über Maven starten (Maven erkennt Ihr Betriebssystem automatisch):
2. Oder Sie starten das generierte JAR-File direkt in einem terminal: java -jar target/<artifactId>-<buildVersion>.jar

## 3. Native Installer erstellen (jpackage)

Mit `jpackage` kann eine native ausführbare Datei oder ein Installer erstellt werden, der eine eigene Java-Laufzeitumgebung (Runtime) mitliefert.

### Befehle nach Betriebssystem

Stellen Sie sicher, dass Sie sich im `target`-Ordner befinden oder den Pfad zu `--input` entsprechend anpassen.

#### Windows (PowerShell)

powershell jpackage `--input .` --name CrazyLabyrinth `--main-jar <artifactId>-<version>.jar` --app-version 1.0.0 `--type app-image` --icon 'classes\packaging\app.ico' `--java-options "--enable-native-access=ALL-UNNAMED"` --java-options "-Dspring.profiles.active=prod"


#### Linux

bash jpackage
--input .
--name CrazyLabyrinth
--main-jar <artifactId>-<version>.jar
--app-version 1.0.0
--type app-image
--icon 'classes/packaging/app.png'
--java-options "--enable-native-access=ALL-UNNAMED"
--java-options "-Dspring.profiles.active=prod"


#### macOs

bash jpackage
--input .
--name CrazyLabyrinth
--main-jar <artifactId>-<version>.jar
--app-version 1.0.0
--type app-image
--icon 'classes/packaging/app.icns'
--java-options "--enable-native-access=ALL-UNNAMED"
--java-options "-Dspring.profiles.active=prod"

### Erläuterung der Typen (`--type`)

*   **app-image**: Erstellt ein Verzeichnis mit der ausführbaren Datei und allen Abhängigkeiten. Dies ist **direkt ausführbar** und eignet sich hervorragend für den schnellen Test oder portable Versionen.
*   **exe / msi (Windows)**, **pkg / dmg (macOS)**, **deb / rpm (Linux)**: Erstellt einen richtigen Installer. 
    *   **Wichtig:** Für die Verteilung von `.exe`, `.msi` oder `.dmg` Dateien ist in der Regel eine **digitale Signierung** erforderlich, da das Betriebssystem die Installation sonst als unsicher blockieren könnte.

### Konfiguration im app-image
Bei einem `app-image` können die Standard-Einstellungen überschrieben werden, indem eine `application.properties` direkt im Hauptverzeichnis des generierten Ordners (neben der ausführbaren Datei) abgelegt wird.

### Hinweise zu Abhängigkeiten
Für die Erstellung von Installern müssen auf dem System eventuell zusätzliche Build-Tools installiert sein:
*   **Windows**: [WiX Toolset](https://wixtoolset.org/) wird für MSI-Installer benötigt.
*   **Linux**: Pakete wie `rpm-build` oder `fakeroot` werden für RPM/DEB benötigt.
*   **macOS**: Xcode Command Line Tools.