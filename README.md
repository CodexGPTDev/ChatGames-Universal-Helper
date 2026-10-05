# ChatGames Universal Helper

Mod de cliente Fabric para resolver desafíos del chat y configurar sus respuestas mediante una interfaz privada. Versión **1.1.0**, desarrollada para **Minecraft Java 1.21.11**.

## Requisitos

| Componente | Versión |
| --- | --- |
| Minecraft Java | 1.21.11 |
| Java | 21 o superior |
| Fabric Loader | 0.18.4 o superior |
| Fabric API | 0.141.3+1.21.11 utilizada para compilar |

El JAR se instala en el cliente del jugador. La GUI se dibuja localmente y sus comandos se registran como comandos de cliente. No hace falta instalar un plugin de interfaz en el servidor.

## Instalación

1. Cierra Minecraft e instala Fabric y Fabric API para 1.21.11.
2. Coloca `chatgames-universal-helper-1.1.0.jar` en `mods`.
3. Retira el JAR anterior del helper y deja una sola versión.
4. Conserva `config/chatgames-universal-helper/` para mantener tus preferencias y respuestas aprendidas.
5. Inicia Minecraft y abre la interfaz con `/cgedit gui`.

Esta actualización conserva los tiempos configurados en 1.0.9. Una configuración más antigua recibe la migración previa a 0–1 segundo una sola vez. No se reactivan juegos que hayas pausado.

## Interfaz privada

Abre `/cgedit gui` o `/cgedit menu` dentro del juego.

- **Tarjetas de minijuegos:** muestran un objeto, el nombre y el tiempo efectivo. Pasa el cursor sobre el objeto o tarjeta para ver el lore, ejemplos, estado y si hereda el tiempo general.
- **Activo / Pausado:** cambia el estado de ese minijuego, lo guarda inmediatamente y cancela su envío pendiente al pausarlo.
- **Clic en la tarjeta:** abre su editor de intervalo.
- **Mod ACTIVADO / EN PAUSA:** controla todas las respuestas; conserva los estados individuales.
- **Tiempo general:** edita el intervalo común de los modos que lo heredan.
- **Avisos:** muestra u oculta el mensaje local «Respuesta enviada». Este control no apaga las respuestas.
- **HUD:** muestra u oculta el panel de estado durante un desafío.
- **Flechas:** cambian de página; el número de tarjetas se adapta a la escala de GUI.

En el editor puedes elegir **tiempo fijo**, **intervalo aleatorio** o **usar tiempo general**. Escribe valores de 0 a 60 segundos, con decimales (`0.5` o `0,5`); también acepta unidades como `500ms`. Hay opciones rápidas de 0, 1, 2, 3 y 5 segundos, además de rangos 0–1 y 3–5.

**Guardar cambios** valida y escribe el ajuste; se conserva al reiniciar. **Cancelar** o **Escape** vuelve sin guardar el intervalo que estabas editando. El mínimo no puede superar al máximo. Si el guardado del intervalo falla, el valor anterior se conserva en memoria.

Los nuevos tiempos se aplican a las próximas respuestas. Una respuesta ya programada conserva su intervalo. Con **0 s** el mod no añade una espera después de detectar y resolver el reto; todavía cuentan la lectura del chat, la agrupación de mensajes, el cálculo y la conexión. La GUI no pausa el servidor ni un mundo local, y el mod puede seguir respondiendo mientras está abierta.

### Capturas de la interfaz

![Menú privado de ChatGames](docs/images/main.png)

![Información de trivia al pasar el cursor](docs/images/lore.png)

![Editor de tiempo de respuesta](docs/images/editor.png)

## Minijuegos de la GUI

| Juego | Función |
| --- | --- |
| Matemáticas | Sumas, restas, multiplicaciones y otras operaciones. |
| Ecuaciones | Resuelve incógnitas de letras y símbolos en varias líneas. |
| Trivia | Banco local y respuestas aprendidas; no copia la pregunta como respuesta. |
| Ordenar palabras | Anagramas de una palabra o frase; conserva la capitalización. |
| Completar palabras | Letras faltantes y escritura esperada por el servidor. |
| Palabra invertida | Invierte el texto solicitado. |
| Reacción | Escribe el texto del reto. |
| Texto aleatorio | Conserva exactamente mayúsculas y minúsculas. |
| Clic en el chat | Utiliza el componente pulsable del evento. |
| Adivinar número | Ajusta los intentos según las pistas; usa el intervalo del modo. |

La interfaz no incorpora HOVERABLE ni HUNT, MINE, PLACE, FISH, EAT, CRAFT o FURNACE. Su implementación anterior permanece fuera del alcance de esta revisión.

Las respuestas desconocidas o ambiguas pueden quedar sin enviar si no hay una solución local fiable. Una IA externa es opcional y solo se consulta cuando está configurada. El banco incluido procede de los desafíos y resultados proporcionados durante el desarrollo; los anuncios posteriores del servidor permiten aprender respuestas.

## Comandos

| Comando | Descripción |
| --- | --- |
| `/cgedit gui` o `/cgedit menu` | Abre tu interfaz privada. |
| `/cgedit on` / `/cgedit off` | Activa o pausa las respuestas; `off` cancela las pendientes. |
| `/cgedit game <tipo> on/off` | Controla un juego específico. |
| `/cgedit settime` | Muestra los intervalos configurados. |
| `/cgedit settime 0s` | Tiempo general sin espera añadida. |
| `/cgedit settime random 0s 1s` | Intervalo general de 0–1 segundo. |
| `/cgedit settime trivia 1s` | Tiempo propio de trivia. |
| `/cgedit settime variable random 3s 5s` | Rango propio para ecuaciones. |
| `/cgedit settime math default` | Hace que matemáticas vuelva a heredar el tiempo general. |
| `/cgedit stats on/off` | Avisos locales de respuesta enviada. |
| `/cgedit debug on/off` | Detalles de detección y envío. |
| `/cgedit status` / `/cgedit last` | Estado del mod y último reto. |
| `/cgedit cache stats` | Cantidad de entradas guardadas. |
| `/cgedit ai status` | Estado de la IA opcional. |
| `/cgedit test <tipo> <texto>` | Prueba una solución sin enviarla. |
| `/cgedit simulate <texto>` | Prueba la detección sin enviar una respuesta. |
| `/cgedit testlog` | Diagnóstico del registro leído. |
| `/cgedit help 1`, `2` o `3` | Ayuda con descripciones y autocompletado. |

El modo llamado RANDOM tiene una sintaxis propia: `/cgedit settime random 0s` fija su tiempo y `/cgedit settime random random 0s 1s` configura su rango. `/cgedit settime random 0s 1s` sigue configurando el rango **general**. Los modos con un intervalo propio lo conservan al cambiar el general.

## Archivos de configuración

Se guardan en `config/chatgames-universal-helper/`:

- `config.json`: activación, modos, tiempo general, intervalos propios, HUD y avisos.
- `trivia.json` y `trivia-cache.json`: respuestas personales y aprendidas.
- `word-cache.json`: respuestas aprendidas para palabras.
- `words-es.txt` y `words-en.txt`: vocabulario personal.

La GUI escribe los mismos ajustes que los comandos. Puedes ampliar tus preguntas y palabras; reinicia después de editar los archivos a mano. No subas configuraciones privadas, registros completos ni credenciales al repositorio.

## Compilar y probar

El proyecto incluye el Gradle Wrapper **9.2.1**. Con Java 21 instalado:

```bash
# Linux / macOS
chmod +x gradlew
./gradlew build
```

```powershell
# Windows
.\gradlew.bat build
```

El JAR instalable se genera en `build/libs/chatgames-universal-helper-1.1.0.jar`. El archivo `*-sources.jar` contiene fuentes y no se instala en `mods`. `build` compila, ejecuta las pruebas JUnit y remapea el mod para Minecraft. `./gradlew runClient` inicia el cliente de desarrollo; necesita entorno gráfico y descarga los recursos del juego.

## Publicar en GitHub

1. Descomprime el ZIP de código fuente.
2. Sube el **contenido de la carpeta del proyecto**: `src/`, `gradle/`, `gradlew`, `gradlew.bat`, archivos Gradle, `README.md`, `LICENSE`, `WORDLIST_NOTICE.md`, `docs/`, `.gitignore` y `.github/`.
3. No subas `build/`, `.gradle/`, `run/`, tus logs ni configuraciones personales. `.gitignore` ya los excluye.
4. El workflow de `.github/workflows/build.yml` compila y ejecuta pruebas cuando se hace push o se abre un pull request.
5. Crea una Release con etiqueta `v1.1.0` y adjunta el JAR instalable como archivo de descarga.

El proyecto no contiene tokens ni claves de API. La IA opcional toma la clave de una variable de entorno configurada por el usuario.

## Estructura

- `gui/`: tarjetas, paginación, editor y borrador de tiempos.
- `command/`: comandos de cliente, ayuda y autocompletado.
- `game/`: detección, sesiones, programación y cancelación de respuestas.
- `solver/`: matemáticas, ecuaciones, trivia y palabras.
- `config/`: preferencias, persistencia y migraciones.
- `chat/`, `log/`, `hud/`, `ai/`: envío, lectura, estado e IA opcional.
- `src/test/`: pruebas de solucionadores, registros, cancelación y configuración de la GUI.

## Licencia y atribuciones

El código del mod se distribuye bajo MIT; consulta [LICENSE](LICENSE). Los diccionarios ampliados son contenido de terceros bajo **CC BY-SA 4.0** y tienen su atribución en [WORDLIST_NOTICE.md](WORDLIST_NOTICE.md). [ANALISIS_LOGS_ES.md](ANALISIS_LOGS_ES.md) conserva el análisis histórico de los retos incorporados en 1.0.7.

## Verificación de 1.1.0

49 pruebas JUnit aprobadas, sin fallos ni pruebas omitidas. La interfaz también se ejecutó en Minecraft 1.21.11 y se revisó en dos tamaños de ventana: lore, guardado y cancelación de intervalos, pausa individual y global, persistencia tras reiniciar, rangos inválidos y paginación. Las capturas de `docs/images/` proceden de esa ejecución del cliente.
