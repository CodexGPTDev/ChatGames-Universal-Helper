package dev.clares.chatgames.command;

import java.util.List;

/** Compact pages keep the command and its explanation together in the chat. */
public final class HelpPages {
    public record Entry(String command, String description) { }
    public record Page(String title, List<Entry> entries) { }
    private static final List<Page> PAGES = List.of(
        new Page("Uso diario", List.of(
            new Entry("/cgedit gui · /cgedit menu", "Abre tu interfaz privada: activa juegos, consulta información y guarda sus tiempos."),
            new Entry("/cgedit on · /cgedit off", "Activa o apaga todas las respuestas automáticas. Al apagar se cancelan los envíos pendientes."),
            new Entry("/cgedit status", "Comprueba si el mod y la lectura del chat están activos."),
            new Entry("/cgedit last", "Muestra el último reto, la respuesta y su estado."),
            new Entry("/cgedit game <tipo> on/off", "Activa o pausa un modo. Ejemplo: /cgedit game unscramble on"),
            new Entry("/cgedit stats on/off", "Muestra u oculta los avisos locales. Para apagar las respuestas usa /cgedit off.")
        )),
        new Page("Tiempos por juego", List.of(
            new Entry("/cgedit settime 0s", "Sin espera añadida. También acepta 1s o 500ms. /cgedit settime muestra todos los intervalos."),
            new Entry("/cgedit settime random 0s 1s", "Responde con una espera de 0 a 1 segundo; intervalo inicial de esta versión."),
            new Entry("/cgedit settime math random 2s 3s", "Configura MATH entre 2 y 3 segundos. Puedes sustituir math por otro tipo."),
            new Entry("/cgedit settime variable random 3s 5s", "Configura las ecuaciones con letras o símbolos entre 3 y 5 segundos."),
            new Entry("/cgedit settime <tipo> default", "Hace que ese modo use el tiempo general. Un tiempo propio se guarda al reiniciar.")
        )),
        new Page("Pruebas y diagnóstico", List.of(
            new Entry("/cgedit test <tipo> <texto>", "Prueba una solución local sin enviarla al servidor. Ejemplo: test unscramble uMics icsD"),
            new Entry("/cgedit simulate <texto>", "Prueba la detección de un mensaje sin responder en el servidor."),
            new Entry("/cgedit testlog", "Muestra el archivo de registro y la última línea de chat leída."),
            new Entry("/cgedit debug on/off", "Activa o desactiva los detalles de detección y envío."),
            new Entry("/cgedit cache stats", "Consulta cuántas respuestas hay guardadas."),
            new Entry("/cgedit ai status", "Consulta el estado y el modelo de la IA opcional.")
        ))
    );
    private HelpPages() { }
    public static int count() { return PAGES.size(); }
    public static Page page(int number) {
        if (number < 1 || number > count()) throw new IllegalArgumentException("Página fuera de rango");
        return PAGES.get(number - 1);
    }
}
