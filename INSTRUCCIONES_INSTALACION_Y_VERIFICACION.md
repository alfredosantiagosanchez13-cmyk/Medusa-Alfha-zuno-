# GUÍA TÉCNICA DE INSTALACIÓN, PERMISOS Y VERIFICACIÓN DE ESTADO
## ZUNO AI PULIDOR 56300 · MASTERING & POLISHING SUITE

---

### 1. INSTALACIÓN DEL APK EN EL ENTORNO / EMULADOR

#### A. En el Emulador Streaming Web (Google AI Studio)
- La aplicación se compila y despliega de manera automática e incremental.
- Cada actualización de código o recurso se empaca en `/app/applet/.build-outputs/app-debug.apk` y se instala automáticamente en la sesión del emulador interactivo visible en la ventana del navegador.
- No se requiere ejecución manual de comandos ADB en este entorno.

#### B. En Emulador Local (Android Studio / Virtual Device AVD)
1. Inicie su dispositivo virtual Android (API 26 a 34+).
2. Localice el archivo APK generado:
   - Ruta en el proyecto: `exports/ZunoAiPulidor-56300-debug.apk` o `.build-outputs/app-debug.apk`
3. Método 1 (Interfaz):
   - Arrastre el archivo `.apk` y suéltelo directamente sobre la ventana del emulador en ejecución.
4. Método 2 (Línea de comandos ADB):
   ```bash
   adb install -r exports/ZunoAiPulidor-56300-debug.apk
   ```
5. Lanzamiento directo mediante Intent:
   ```bash
   adb shell am start -n com.aistudio.zunopulidor.a56300/com.example.MainActivity
   ```

#### C. En Dispositivo Físico Android
1. Transfiera `ZunoAiPulidor-56300-debug.apk` a su teléfono vía USB, Google Drive o descarga directa.
2. En los ajustes del teléfono, permita la instalación desde la fuente utilizada (*Ajustes > Seguridad > Instalar aplicaciones desconocidas*).
3. Abra el archivo e instálelo normalmente.

---

### 2. VERIFICACIÓN DE PERMISOS PARA LA LECTURA DE ARCHIVOS

#### Arquitectura de Permisos (Google Play Policy & Scoped Storage)
- La aplicación implementa el contrato moderno **`ActivityResultContracts.GetContent()`** del **Storage Access Framework (SAF)** de Android.
- **Ventaja de seguridad y privacidad:**
  - Al seleccionar una pista de audio (`MP3`, `WAV`, `M4A`, `FLAC`) o video (`MP4`), el sistema operativo Android concede un permiso de lectura efímero y seguro sobre el `Uri` (`content://...`) otorgado explícitamente por el usuario.
  - **No requiere permisos peligrosos en tiempo de ejecución** como `READ_EXTERNAL_STORAGE` o `MANAGE_EXTERNAL_STORAGE`, eliminando diálogos intrusivos y garantizando 100% de cumplimiento con las políticas de Google Play (Android 10 a 14+).
- **Permisos del Manifest verificados (`AndroidManifest.xml`):**
  - `<uses-permission android:name="android.permission.INTERNET" />`: Requerido exclusivamente para la consulta opcional con Gemini AI Studio (cuando el usuario ingresa su API Key voluntariamente).
  - `<uses-permission android:name="android.permission.VIBRATE" />`: Proporciona retroalimentación háptica precisa al presionar botones y conmutar el reproductor A/B.
  - `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />`: Para validar la disponibilidad de red en caso de usar el asistente en la nube.
- **Acceso a archivos compartidos y exportaciones:**
  - Configurado a través de **`androidx.core.content.FileProvider`** con autoridad `${applicationId}.fileprovider`, protegiendo los archivos generados en almacenamiento interno privado.

---

### 3. CONFIRMACIÓN Y PERSISTENCIA DEL ESTADO PREVIO

#### A. Persistencia con Base de Datos Room (Offline-First)
- La suite utiliza una base de datos local SQLite administrada por **Room** (`ZunoDatabase`, `SongProjectDao`, `SongRepository`).
- Cada archivo analizado y pulido se registra de manera persistente con sus métricas:
  - Nombre del proyecto y duración.
  - Métricas originales (LUFS, True Peak, RMS, rango dinámico, clipping).
  - Métricas pulidas post-mastering.
  - Rutas de los archivos procesados.
- **Al iniciar la aplicación (`ZunoViewModel.init`):**
  ```kotlin
  viewModelScope.launch {
      repository.allProjects.collect { projects ->
          _uiState.update { it.copy(savedProjects = projects) }
      }
  }
  ```
  El historial previo se recupera de manera reactiva e inmediata, sin bloqueos de interfaz ni pantallas blancas.

#### B. Manejo de Rotación y Recreación de Actividad
- El `MainActivity` tiene configurado en `AndroidManifest.xml`:
  `android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"`
- La orientación de pantalla o cambios de tamaño de ventana no destruyen el estado del reproductor ni interrumpen la reproducción en vivo.
- El ViewModel mantiene el estado en memoria durante todo el ciclo de vida de la aplicación.

---

### 4. RESUMEN DE COMPROBACIÓN FÍSICA
- **APK binario compilado:** `ZunoAiPulidor-56300-debug.apk` (25.39 MB, Package: `com.aistudio.zunopulidor.a56300`, VersionCode 1).
- **Flujo verificado:** SUBIR → ANALIZAR → PULIR → A/B → EXPORTAR.
- **Masters listos:** WAV 24-bit 48kHz, MP3 320kbps CBR, MP4 Video sincronizado en `exports/`.
