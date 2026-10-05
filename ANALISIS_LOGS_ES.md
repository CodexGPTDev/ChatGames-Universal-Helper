# ChatGames Universal Helper 1.0.7 — revisión de registros

Se leyeron los 160 archivos `.log.gz` de `Logs(1).zip`, con fechas del 23 de julio al 30 de septiembre de 2026. Se asociaron las preguntas con el anuncio de ganador o de respuesta correcta de la misma ronda. Se descartaron los mensajes del mod y los intentos de jugadores como fuente de respuestas.

La base incorpora **85 preguntas distintas de TRIVIA** confirmadas en 139 rondas, **110 patrones distintos de FILLOUT** confirmados en 114 rondas y **136 anagramas distintos** confirmados en 139 rondas. Las respuestas se conservan tal como las espera el servidor, incluidas sus mayúsculas, acentos y preguntas con errores de escritura. Algunas respuestas configuradas por los servidores difieren de la respuesta que sugeriría una pregunta general; para el juego se usa la que el servidor anunció.

## Activar y apagar el mod

| Comando | Resultado |
| --- | --- |
| `/cgedit on` | Activa las respuestas automáticas para las próximas rondas. |
| `/cgedit off` | Apaga las respuestas y cancela los envíos pendientes, incluso si ya llegaron a la cola del cliente. |
| `/cgedit game unscramble off` | Apaga ese modo y cancela su respuesta pendiente. |
| `/cgedit stats off` | Oculta los avisos locales «Respuesta enviada». El comando explica que `/cgedit off` apaga las respuestas. |
| `/cgedit status` | Muestra por separado el estado de las respuestas automáticas y el de los avisos. |

La activación global queda guardada al reiniciar. Apagar y volver a activar no reanuda respuestas de una ronda anterior. Se mantiene el autocompletado y la ayuda por páginas.

## Fallos encontrados y cambios

- El FILLOUT `L_v_r` recibió «Lavar»; el servidor esperaba **Lever**. Las respuestas confirmadas se consultan antes de un diccionario general y se importan al actualizar las cachés antiguas.
- TRIVIA podía perder preguntas partidas en dos líneas, como la del portal al End y la de las mesas de trabajo. Ahora se reúne el texto completo.
- La pregunta `What item is dropped when a turtle dies by lightning` llega sin `?`; ahora se reconoce por la instrucción explícita de TRIVIA.
- FILLOUT podía cortar frases en el primer fragmento. Se preserva el patrón completo de **Caja registradora** y sus espacios.
- Los cierres `was the fastest to fill` y `was the fastest to get` ahora cierran y cancelan la ronda, al igual que los cierres de UNSCRAMBLE/UNREVERSE ya corregidos.
- Se mantienen los avisos automáticos apagados por defecto y las soluciones de MATH existentes.

## Límites observados en estos registros

El patrón `_a_a` tuvo tres respuestas anunciadas: **Lava** en el formato español de 20 segundos y **Cama/Caja** en el formato de 60 segundos. El mod distingue ambos formatos; cuando dos respuestas comparten el mismo patrón y formato, usa la más reciente confirmada (**Caja**) y aprende los anuncios posteriores del servidor. Esa máscara por sí sola no permite determinar una única palabra.

La pregunta `What food should you never feed a parrot?` no incluye un anuncio de respuesta correcta en la ronda registrada. La pregunta sobre el idioma de Brasil tampoco incluye cierre; se conserva **Portugués**, deducido en la versión anterior y señalado como no confirmado. No se inventan respuestas nuevas de servidor. Una pregunta desconocida sin IA configurada no se envía como si fuera su propia solución.

## TRIVIA confirmada

| Pregunta | Respuesta exacta | Registro y línea del resultado |
| --- | --- | --- |
| ¿Cuántos lados tiene un dado? (número) | 6 | `2026-07-23-5.log:365` |
| ¿Cuál es el animal que dice "mu" y produce leche? | Vaca | `2026-07-24-5.log:1034` |
| ¿Qué instrumento musical se toca golpeando con baquetas? | Batería | `2026-07-24-6.log:426` |
| ¿Qué fruta es amarilla por fuera y blanca por dentro, y se puede utilizar para hacer helado? | Plátano | `2026-07-24-6.log:801` |
| ¿En qué planeta vivimos? | Tierra | `2026-07-25-2.log:420` |
| ¿Qué color resulta de mezclar azul y rojo? | Morado | `2026-07-27-1.log:31687` |
| ¿Cómo se llama el lugar donde los aviones despegan y aterrizan? | Aeropuerto | `2026-07-27-4.log:1922` |
| ¿Qué animal tiene alas y vuela en el cielo? | Pájaro | `2026-07-27-4.log:2527` |
| ¿Cómo se llama el instrumento musical que se toca golpeando con las manos? | Tambor | `2026-07-27-4.log:3290` |
| ¿Cuál es la parte del cuerpo que usamos para caminar? | Pierna | `2026-07-28-1.log:609` |
| ¿Qué fruta es pequeña, redonda y de color rojo? | Fresa | `2026-07-28-2.log:434` |
| ¿Cuál es la capital de España? | Madrid | `2026-07-29-3.log:881` |
| ¿Qué animal tiene una concha y se arrastra lentamente? | Caracol | `2026-07-30-1.log:431` |
| ¿Cuál es el color que resulta de mezclar rojo y amarillo? | Naranja | `2026-07-30-1.log:683` |
| ¿Qué color se obtiene de mezclar azul y rojo? | Morado | `2026-07-30-2.log:707` |
| ¿Qué animal tiene una trompa larga y se utiliza para cargar cosas pesadas? | Elefante | `2026-07-30-2.log:1117` |
| ¿Qué animal tiene manchas negras y blancas y vive en África? | Cebra | `2026-07-30-2.log:1302` |
| ¿Cómo se llama el proceso de cambio de estado del agua de sólido a líquido? | Fusión | `2026-07-30-2.log:1860` |
| ¿Qué animal tiene plumas y puede volar? | Pájaro | `2026-07-30-3.log:444` |
| ¿Qué color tiene el cielo en un día despejado? | Azul | `2026-07-30-3.log:845` |
| ¿Cuál es el animal que dice "mu" y nos da leche? | Vaca | `2026-07-30-4.log:475` |
| ¿Cómo se llama el objeto que usamos para escribir en un papel? | Lápiz | `2026-07-30-4.log:987` |
| ¿Cómo se llama el lugar donde se guardan los libros en una escuela? | Biblioteca | `2026-07-31-1.log:586` |
| ¿Qué bloque transparente se fabrica fundiendo arena? | Vidrio | `2026-08-15-1.log:1288` |
| ¿Qué mob verde explota cuando se acerca a un jugador? | Creeper | `2026-08-15-2.log:589` |
| ¿Qué objeto se necesita para encender un portal al Nether? | Mechero | `2026-08-15-3.log:449` |
| ¿Qué animal puede ser montado usando una silla? | Caballo | `2026-08-15-3.log:464` |
| ¿Cómo se llama el lugar donde se van los niños a aprender? | Escuela | `2026-09-01-1.log:421` |
| ¿Qué animal tiene una trompa larga y es el más grande de la Tierra? | Elefante | `2026-09-07-1.log:956` |
| ¿Cuántas estaciones del año hay? | Cuatro | `2026-09-07-1.log:1208` |
| ¿Cómo se llama el órgano que utilizamos para oír? | Oido | `2026-09-07-3.log:834` |
| ¿Qué animal tiene una cola larga y se cuelga de los árboles? | Mono | `2026-09-08-2.log:488` |
| ¿Cómo se llama el resultado de sumar dos más dos? | Cuatro | `2026-09-20-1.log:502` |
| ¿Qué fruta es de color rojo y se utiliza para hacer jugo? | Fresa | `2026-09-20-1.log:599` |
| ¿Cuántas patas tiene un perro? | Cuatro | `2026-09-21-1.log:353` |
| ¿Qué fruta es amarilla por fuera y tiene muchas semillas en su interior? | Sandía | `2026-09-21-1.log:527` |
| ¿Cuál es la capital de Francia? | Paris | `2026-09-21-2.log:547` |
| ¿Cómo se llama el lugar donde se guardan los libros en la escuela? | Biblioteca | `2026-09-21-2.log:793` |
| ¿Qué animal tiene una melena y ruge en la selva? | León | `2026-09-21-2.log:858` |
| ¿Qué animal tiene una cola larga y peluda y vive en los árboles? | Mono | `2026-09-21-2.log:929` |
| ¿Qué animal es famoso por su gran trompa y memoria? | Elefante | `2026-09-21-3.log:558` |
| ¿Qué animal tiene plumas y vuela por el aire? | Ave | `2026-09-21-4.log:839` |
| ¿Cómo se llama el lugar donde guardamos la comida en la cocina? | Refrigerador | `2026-09-21-4.log:911` |
| ¿Cuántos meses tiene un año? | Doce | `2026-09-22-1.log:552` |
| ¿Qué animal es conocido por su melena y ruge en la selva? | León | `2026-09-22-1.log:1990` |
| ¿Cuántos dedos tiene una mano? | Cinco | `2026-09-22-2.log:1584` |
| ¿Cuál es la capital de Alemania? | Berlín | `2026-09-22-2.log:1702` |
| ¿Qué animal tiene rayas negras y blancas y es conocido por su agilidad? | Cebra | `2026-09-22-2.log:2266` |
| ¿Qué instrumento se toca golpeando con baquetas? | Batería | `2026-09-23-2.log:3707` |
| ¿Qué animal tiene una caparazón y se arrastra lentamente? | Tortuga | `2026-09-23-2.log:4149` |
| ¿Qué animal tiene rayas negras y blancas y vive en la selva? | Tigre | `2026-09-23-2.log:5245` |
| ¿Cuál es el animal que dice "guau" y es amigo del hombre? | Perro | `2026-09-23-4.log:2601` |
| ¿Cómo se llama el instrumento musical de viento que se toca soplando por una boquilla? | Flauta | `2026-09-23-5.log:3956` |
| ¿Qué animal tiene una cola larga y es conocido por colgarse de los árboles? | Mono | `2026-09-23-5.log:4434` |
| ¿Cuál es el día que viene después del viernes? | Sábado | `2026-09-24-2.log:1587` |
| ¿Qué fruta es pequeña, roja y muy dulce? | Fresa | `2026-09-24-2.log:2164` |
| ¿Qué fruta es de color amarillo por fuera y verde por dentro? | Sandía | `2026-09-24-2.log:2904` |
| ¿Cómo se llama el vehículo que usamos para viajar por la ciudad? | Auto | `2026-09-24-2.log:4553` |
| ¿Cuál es el opuesto de "frío"? | Calor | `2026-09-24-2.log:5600` |
| ¿Qué animal dice "miau"? | Gato | `2026-09-25-3.log:648` |
| ¿Qué fruta es amarilla por fuera y blanca por dentro? | Plátano | `2026-09-25-4.log:1306` |
| ¿Qué color resulta de mezclar rojo y amarillo? | Naranja | `2026-09-25-4.log:2261` |
| ¿Qué color tiene la sangre de las personas cuando sale del cuerpo? | Rojo | `2026-09-27-1.log:742` |
| ¿Qué animal es conocido por tener una cola larga y peluda? | Mono | `2026-09-27-1.log:1519` |
| ¿Cómo se llama el lugar donde se enseña y aprende? | Escuela | `2026-09-27-2.log:802` |
| ¿Cuál es el color que resulta de mezclar amarillo y blanco? | Amarillo | `2026-09-27-2.log:1589` |
| ¿Qué animal tiene una cola larga y se cuelga de las ramas? | Mono | `2026-09-27-2.log:1971` |
| ¿Qué animal tiene una coraza y se mueve muy despacio? | Tortuga | `2026-09-27-2.log:2132` |
| ¿Cuál es el metal más precioso del mundo? | Oro | `2026-09-28-1.log:940` |
| What itme do you get for smelting a wood log? | Charcoal | `2026-09-28-2.log:426` |
| How many eyes of ender are needed to open the end portal? | 12 | `2026-09-28-2.log:440` |
| How many blocks does it take to fully power 1 beacon? | 169 | `2026-09-28-2.log:636` |
| How many music discs are in Minecraft? | 15 | `2026-09-28-2.log:1033` |
| What item is dropped when a turtle dies by lightning | Bowl | `2026-09-28-3.log:934` |
| What material is used to tame cats? | Fish | `2026-09-28-4.log:439` |
| What biome is home to bamboo? | Jungle | `2026-09-29-1.log:1006` |
| What item does the wither drop when slain? | Nether Star | `2026-09-29-1.log:1019` |
| What valueable item drops from the evoker? | Totem of Undying | `2026-09-29-1.log:1032` |
| How many wooden planks does it take to craft 16 crafting tables? | 64 | `2026-09-29-1.log:1060` |
| What is the most common crafting material in Minecraft? | Wood | `2026-09-29-1.log:1076` |
| ¿Qué animal tiene una trompa larga y vive en la selva? | Elefante | `2026-09-29-5.log:4108` |
| ¿Cómo se llama el cuerpo celeste que brilla en la noche y gira alrededor de la Tierra? | Luna | `2026-09-29-6.log:2400` |
| ¿Cuál es el opuesto de "lento"? | Rápido | `2026-09-29-6.log:3462` |
| ¿Cuántos días tiene el mes de abril? | Treinta | `2026-09-29-6.log:3793` |
| ¿Cómo se llama el lugar donde compramos comida y otras cosas? | Mercado | `2026-09-29-6.log:3923` |

Se conserva además la variante del dado sin el sufijo `(número)`; la pregunta del ZIP lleva ese sufijo.

## FILLOUT confirmado

| Patrón completo | Respuesta exacta | Formato observado |
| --- | --- | --- |
| `_a_a` | Cama | Español, 60 s |
| `_a_a` | Lava | Español, 20 s |
| `_a_a` | Caja | Español, 60 s |
| `_ome_a` | Cometa | Español, 60 s |
| `_e_ua` | Yegua | Español, 60 s |
| `_li__te` | Cliente | Español, 60 s |
| `_erdo` | Cerdo | Español, 60 s |
| `T_e__a` | Tierra | Español, 60 s |
| `C___ivo` | Cultivo | Español, 60 s |
| `_ofre` | Cofre | Español, 20 s |
| `_o_s` | Mobs | Español, 20 s |
| `_no_oro` | Inodoro | Español, 60 s |
| `__va_o` | Lavabo | Español, 60 s |
| `___tina` | Cortina | Español, 60 s |
| `__a_tor` | Tractor | Español, 60 s |
| `_n_do_o` | Inodoro | Español, 60 s |
| `_ava` | Lava | Español, 20 s |
| `_ul_i_o` | Cultivo | Español, 60 s |
| `_e_ul__a` | Nebulosa | Español, 60 s |
| `G__lo` | Gallo | Español, 60 s |
| `__bs` | Mobs | Español, 20 s |
| `_r_eper` | Creeper | Español, 20 s |
| `Dia_an_e` | Diamante | Español, 20 s |
| `_ep__no` | Neptuno | Español, 60 s |
| `__co` | Pico | Español, 20 s |
| `_ostr__or` | Mostrador | Español, 60 s |
| `_ase` | Fase | Español, 60 s |
| `_st__te` | Estante | Español, 60 s |
| `__te_rito` | Meteorito | Español, 60 s |
| `_asillo` | Pasillo | Español, 60 s |
| `__laxia` | Galaxia | Español, 60 s |
| `_l__mb_a` | Alfombra | Español, 60 s |
| `A_m_cén` | Almacén | Español, 60 s |
| `__anero` | Granero | Español, 60 s |
| `F__e` | Fase | Español, 60 s |
| `__l_pse` | Eclipse | Español, 60 s |
| `__aje` | Viaje | Español, 60 s |
| `__erto` | Huerto | Español, 60 s |
| `_evera` | Nevera | Español, 60 s |
| `Co__os` | Cosmos | Español, 60 s |
| `_ie__a` | Tierra | Español, 60 s |
| `L__a_o` | Lavabo | Español, 60 s |
| `Un_v_rso` | Universo | Español, 60 s |
| `Fa_e` | Fase | Español, 60 s |
| `Ab__a` | Abeja | Español, 60 s |
| `_aja _e_is___dora` | Caja registradora | Español, 60 s |
| `__t__rito` | Meteorito | Español, 60 s |
| `_a_él__e` | Satélite | Español, 60 s |
| `___omb_a` | Alfombra | Español, 60 s |
| `C_l_ivo` | Cultivo | Español, 60 s |
| `__rtera` | Cartera | Español, 60 s |
| `_iaje` | Viaje | Español, 60 s |
| `V_n_s` | Venus | Español, 60 s |
| `_á_para` | Lámpara | Español, 60 s |
| `A_mar_o` | Armario | Español, 60 s |
| `_r_ita` | Órbita | Español, 60 s |
| `__scue__o` | Descuento | Español, 60 s |
| `_i_rina` | Vitrina | Español, 60 s |
| `__té_ite` | Satélite | Español, 60 s |
| `E_t_nte` | Estante | Español, 60 s |
| `_est__u_o` | Vestíbulo | Español, 60 s |
| `_rano` | Urano | Español, 60 s |
| `_guje_o` | Agujero | Español, 60 s |
| `Co___e` | Cohete | Español, 60 s |
| `Ne__uno` | Neptuno | Español, 60 s |
| `P__n__o_de` | Planetoide | Español, 60 s |
| `_ereal` | Cereal | Español, 60 s |
| `T_a_le_o` | Toallero | Español, 60 s |
| `__tél_te` | Satélite | Español, 60 s |
| `P__o` | Polo | Español, 60 s |
| `__evo` | Huevo | Español, 60 s |
| `_oh__e` | Cohete | Español, 60 s |
| `__ie_te` | Cliente | Español, 60 s |
| `_a___ite` | Satélite | Español, 60 s |
| `_am_o` | Campo | Español, 60 s |
| `__rtina` | Cortina | Español, 60 s |
| `_aj__o` | Cajero | Español, 60 s |
| `Es____la` | Estrella | Español, 60 s |
| `_aja` | Caja | Español, 60 s |
| `___ante` | Estante | Español, 60 s |
| `_a_a __gis_ra_ora` | Caja registradora | Español, 60 s |
| `_olo` | Polo | Español, 60 s |
| `_a__a_ión` | Radiación | Español, 60 s |
| `Ve_t_na` | Ventana | Español, 60 s |
| `S__ador` | Secador | Español, 60 s |
| `_a_a r__i__rad_ra` | Caja registradora | Español, 60 s |
| `___ero` | Cajero | Español, 60 s |
| `S__éli_e` | Satélite | Español, 60 s |
| `_i_t_ma` | Sistema | Español, 60 s |
| `_e_i_lo` | Cepillo | Español, 60 s |
| `T___ole` | Tadpole | Inglés, 20 s |
| `A__lot_` | Axolotl | Inglés, 20 s |
| `Q_a_tz` | Quartz | Inglés, 20 s |
| `Sh__ker` | Shulker | Inglés, 20 s |
| `Nethe__te` | Netherite | Inglés, 20 s |
| `B_t_on` | Button | Inglés, 20 s |
| `Ma_g_ove` | Mangrove | Inglés, 20 s |
| `End__m_n` | Enderman | Inglés, 20 s |
| `Che_t` | Chest | Inglés, 20 s |
| `_or_e` | Horse | Inglés, 20 s |
| `_abbi_` | Rabbit | Inglés, 20 s |
| `Ho__er` | Hopper | Inglés, 20 s |
| `_c_i_se` | Eclipse | Español, 60 s |
| `_omp_ar` | Comprar | Español, 60 s |
| `S_d__al` | Sideral | Español, 60 s |
| `__cador` | Secador | Español, 60 s |
| `_rbita` | Órbita | Español, 60 s |
| `_enus` | Venus | Español, 60 s |
| `__la_ia` | Galaxia | Español, 60 s |
| `_es__b_lo` | Vestíbulo | Español, 60 s |
| `Crá__r` | Cráter | Español, 60 s |
| `L_v_r` | Lever | Inglés, 20 s |

## Instalación

Minecraft **1.21.11**, Fabric Loader **0.18.4**, Fabric API **0.141.3+1.21.11** y Java **21**. Cierra el juego, sustituye el JAR anterior por `chatgames-universal-helper-1.0.7.jar` en `mods` y vuelve a iniciar. Deja una sola versión del helper instalada. No borres `config/chatgames-universal-helper`.

Se verifican los cambios con pruebas automáticas de soluciones, corpus completo, detección, cancelación y conteo de envíos mediante un emisor de prueba. No se realizó una conexión al servidor real desde este entorno.

Resultado de la verificación: **32 pruebas aprobadas, 0 fallos y 0 errores**, con **392 rondas confirmadas** usadas como casos de regresión. La compilación completa finalizó correctamente y se verificaron los recursos y metadatos del JAR instalable.
