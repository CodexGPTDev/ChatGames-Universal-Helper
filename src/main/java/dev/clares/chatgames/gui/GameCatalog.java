package dev.clares.chatgames.gui;

import dev.clares.chatgames.game.GameType;
import java.util.List;

/** Only the answering modes requested for this server are offered in the GUI. */
public final class GameCatalog {
    public record Entry(GameType type,String title,String description,String detail) {
        public String key(){return type.name().toLowerCase(java.util.Locale.ROOT);}
    }
    public static final List<Entry> ENTRIES=List.of(
        new Entry(GameType.MATH,"Matemáticas","Resuelve sumas, restas, multiplicaciones y otras operaciones.","Ejemplo: 6 × 7 → 42"),
        new Entry(GameType.VARIABLE,"Ecuaciones","Resuelve incógnitas con letras o símbolos en varias líneas.","Espera a recibir las ecuaciones completas."),
        new Entry(GameType.TRIVIA,"Trivia","Responde preguntas con el banco local y las respuestas aprendidas.","Una pregunta desconocida necesita una solución fiable."),
        new Entry(GameType.UNSCRAMBLE,"Ordenar palabras","Ordena letras de una palabra o frase y conserva su capitalización.","Ejemplo: uMics icsD → Music Disc"),
        new Entry(GameType.FILLOUT,"Completar palabras","Completa letras faltantes con la escritura esperada por el servidor.","Ejemplo: _abbi_ → Rabbit"),
        new Entry(GameType.UNREVERSE,"Palabra invertida","Invierte el texto del reto y envía la palabra resultante.","Ejemplo: ssalgypS → Spyglass"),
        new Entry(GameType.REACTION,"Reacción","Escribe el texto solicitado por el evento de reacción.","Con 0 s no se añade una espera al resolver."),
        new Entry(GameType.RANDOM,"Texto aleatorio","Copia el texto del reto respetando mayúsculas y minúsculas.","Ejemplo: aJCOfw → aJCOfw"),
        new Entry(GameType.CLICKABLE,"Clic en el chat","Usa el componente pulsable enviado por el minijuego.","Mantiene el comportamiento de CLICKABLE."),
        new Entry(GameType.GUESS_THE_NUMBER,"Adivinar número","Ajusta los intentos según las pistas de mayor o menor.","Cada intento usa el intervalo de este modo.")
    );
    private GameCatalog(){}
}
