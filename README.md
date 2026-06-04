# Snake Race — ARSW Lab #2 (Java 21, Virtual Threads)

**Escuela Colombiana de Ingeniería – Arquitecturas de Software**  
Laboratorio de programación concurrente: condiciones de carrera, sincronización y colecciones seguras.

---

## Requisitos

- **JDK 21** (Temurin recomendado)
- **Maven 3.9+**
- SO: Windows, macOS o Linux

---

## Cómo ejecutar

```bash
mvn clean verify
mvn -q -DskipTests exec:java -Dsnakes=4
```

- `-Dsnakes=N` → inicia el juego con **N** serpientes (por defecto 2).
- **Controles**:
  - **Flechas**: serpiente **0** (Jugador 1).
  - **WASD**: serpiente **1** (si existe).
  - **Espacio** o botón **Action**: Pausar / Reanudar.

---

## Reglas del juego (resumen)

- **N serpientes** corren de forma autónoma (cada una en su propio hilo).
- **Ratones**: al comer uno, la serpiente **crece** y aparece un **nuevo obstáculo**.
- **Obstáculos**: si la cabeza entra en un obstáculo hay **rebote**.
- **Teletransportadores** (flechas rojas): entrar por uno te **saca por su par**.
- **Rayos (Turbo)**: al pisarlos, la serpiente obtiene **velocidad aumentada** temporal.
- Movimiento con **wrap-around** (el tablero “se repite” en los bordes).

---

## Arquitectura (carpetas)

```
co.eci.snake
├─ app/                 # Bootstrap de la aplicación (Main)
├─ core/                # Dominio: Board, Snake, Direction, Position
├─ core/engine/         # GameClock (ticks, Pausa/Reanudar)
├─ concurrency/         # SnakeRunner (lógica por serpiente con virtual threads)
└─ ui/legacy/           # UI estilo legado (Swing) con grilla y botón Action
```

---

# Actividades del laboratorio


## Solución Parte I

Se modificó el programa PrimeFinder para que cada t milisegundos (dados por el usuario), todos los hilos de la clase PrimeFinderThread se pausen y impriman todos los numeros primos que han encontrado hasta el momento. 

Para esto se creó la clase Timer. Esta se corre como otro hilo, al mismo tiempo que los hilos para buscar primos, que va contando el tiempo hasta que pasen t milisegundos. Cuando pasa el tiempo requerido, invoca el método de pauseThread, el cual es Synchronized y usa wait(), además cambia la bandera paused a true, esta bandera se usa en el ciclo dentro de run(). Luego Timer le dice a cada hilo que imprima cuantos primos lleva, y luego invoca el método resumeThread que usa notify() y es Synchronized, además cambia la bandera paused a false.

Ejecutar programa parte 1:

```bash
javac -d wait-notify-excercise/target/classes wait-notify-excercise/src/main/java/edu/eci/arsw/primefinder/*.java; java -cp wait-notify-excercise/target/classes edu.eci.arsw.primefinder.Main
```

---

## Solución Parte II

1) Análisis de concurrencia:

El código usa hilos en la clase SnakeRunner, al hacer que esta extienda de la interfaz Runnable. Esta clase crea una serpiente y le asigna un tablero que es compartido para todas las serpientes. 
Ya que el tablero es compartido, es necesario que los métodos dentro del tablero que lo modifican sean sincronizados para que dos serpientes no editen algo al mismo tiempo o una interactúe con algo que ya no está.

Dentro de la clase Board se tienen los atributos mice, obstacles y turbo que son HashSets y teleports que es un HashMap. Estas colecciones no son seguras por sí solas en un contexto recurrente.
Sin embargo, para leer estos atributos del objeto, hay que usar los métodos mice(), obstacles(), turbo() y teleports(), los cuales son métodos sincronizados y devuelven una copia del atributo. Esto ayuda a que 
no se lea mientras se está modificando.

Por otra parte, la clase Snake, no extiende Thread ni Runnable, pero si podría llegar a ser leida o modificada por más de un hilo a la vez si queremos implementar colisiones entre serpientes por ejemplo. Por lo que 
deberia protegerse ante un contexto concurrente. Primeramente, el atributo body, es de tipo ArrayDeque, el cual no está protegido contra la recurrencia, y los metodos para leer este atributo tampoco usan sincronización.
La clase GamePanel lee directamente la serpiente con snapshot() que no está protegido, lo que puede hacer que se muestre una cosa mientras que realmente otras clases como Board están modificando el estado de la serpiente.

Por último en la clase SnakeRunner, en el método run, en la última línea del ciclo se hace un Thread.sleep(), lo cual se detecta como un posible caso de 
busy waiting, sin embargo, en este caso se usa para establecer la velocidad en la que se van a actualizar la posición de las serpientes, lo cual es útil en este caso
para que el juego sea más fácil de jugar.

2) Correcciones mínimas y regiones críticas

En la clase Snake se cambiaron los métodos direction(), turn(), head(), snapshot() y advance() para que fueran sincronizados. Esto se hizo porque body usa un ArrayDeque, que no es seguro en un entorno concurrente. 
No se cambió la estructura porque el problema no era solamente el tipo de colección, sino que varios hilos podían leer o modificarla al mismo tiempo. Por eso se protegieron los métodos que acceden a ella.

Con esto se evita que, por ejemplo, la UI haga un snapshot() mientras un SnakeRunner está ejecutando advance(), lo que podría generar lecturas inconsistentes.

También se revisó Board. Aunque usa colecciones como HashSet y HashMap, sus métodos públicos ya estaban sincronizados y devuelven copias, por lo que no se expone directamente el estado interno. Además, step() está sincronizado, 
lo cual es necesario porque modifica el tablero compartido.

Adicionalmente, se implementó una pausa real para los SnakeRunner. Antes solo se pausaba el GameClock, entonces la UI dejaba de renderizar, pero las serpientes seguían moviéndose internamente. Ahora cada serpiente tiene un estado paused, 
y el SnakeRunner llama a waitIfPaused() al inicio de su ciclo. Si la serpiente está pausada, el hilo hace wait() y queda bloqueado sin hacer espera activa. Cuando se llama a resume(), se cambia paused a false y se usa notifyAll() para continuar.

3) Control de ejecución seguro UI

Se modificó el control de la UI para manejar iniciar, pausar y reanudar el juego. La pausa ya no afecta solo al renderizado, sino también a los hilos que mueven las serpientes.
 
Como la suspensión no es instantánea, se agregó un PauseController para coordinar el momento en que todos los SnakeRunner ya llegaron a un punto seguro. Esto es importante porque al presionar pausa algún hilo podría estar todavía dentro de board.step(snake) o esperando en el Thread.sleep().

Cuando se pausa, la UI llama a requestPause(snakes.size()). Luego cada SnakeRunner, al llegar a la revisión de pausa, avisa que ya está detenido. La UI espera con awaitAllPaused() hasta que todos hayan confirmado. Esta espera se hace en un hilo aparte para no bloquear la interfaz de Swing.

Después de que todos los runners están pausados, se actualiza la UI con SwingUtilities.invokeLater(). En ese momento se calcula la serpiente viva más larga usando max() y la peor serpiente usando min() sobre las serpientes que ya murieron. La información se muestra en un JLabel en la parte inferior de la ventana.
 
También se agregó la lógica para que las serpientes puedan morir correctamente. Para esto se añadió un estado de vida en la clase Snake, y en Board se agregó un nuevo resultado de movimiento llamado HIT_SNAKE. El tablero ahora conoce las serpientes y puede revisar si la siguiente posición de una serpiente choca 
contra el cuerpo de ella misma o contra otra serpiente. Cuando ocurre esta colisión, la serpiente se marca como muerta y deja de avanzar.
 
Esto era necesario porque antes el juego no tenía una forma clara de saber cuáles serpientes seguían vivas ni cuál había muerto primero. Con este cambio ya se puede calcular correctamente la serpiente viva más larga y la peor serpiente.
 
Con esto se reduce el riesgo de tearing, es decir, que la UI muestre datos mezclados de momentos diferentes del juego. Ahora los datos se leen cuando los hilos ya no están modificando el estado. Además, como las serpientes tienen estado de vida/muerte, la información mostrada corresponde mejor al estado real del juego.

4) Robustez bajo carga

Para probar la robustez se ejecutó el juego con varias serpientes usando -Dsnakes=20 y también con valores mayores como -Dsnakes=30. Además, se redujo el tiempo del GameClock para aumentar la frecuencia de actualización.

Durante las pruebas se encontró un problema cuando las serpientes morían. En SnakeRunner el ciclo usaba ||:
```
while (!Thread.currentThread().isInterrupted() || snake.isAlive())
```
Esto hacía que el hilo pudiera seguir ejecutándose aunque la serpiente ya estuviera muerta. Además, al detectar HIT_SNAKE se hacía continue, por lo que el hilo podía volver a ejecutar el ciclo sin pasar por el Thread.sleep(). Bajo carga, esto causaba que muchos hilos siguieran compitiendo por el tablero.

Se corrigió cambiando la condición a:

```
while (!Thread.currentThread().isInterrupted() && snake.isAlive())
```
y haciendo que el hilo termine cuando la serpiente muere. También se ajustó la pausa para contar solo las serpientes vivas, ya que las muertas ya no tienen un SnakeRunner que pueda confirmar la pausa.

Finalmente, se verificó que las colecciones compartidas siguieran protegidas con synchronized, especialmente en Snake y Board. También se revisó que los teleports y el turbo no generaran carreras, ya que los teleports se consultan dentro de step() y el turbo solo cambia la velocidad del hilo.

Después de estos cambios, el juego funcionó correctamente con -Dsnakes=30, sin bloqueos, sin ConcurrentModificationException y sin lecturas inconsistentes visibles.


## Cómo correr pruebas

```bash
mvn clean verify
```

Incluye compilación y ejecución de pruebas JUnit. Si tienes análisis estático, ejecútalo en `verify` o `site` según tu `pom.xml`.

---

## Créditos

Este laboratorio es una adaptación modernizada del ejercicio **SnakeRace** de ARSW. El enunciado de actividades se conserva para mantener los objetivos pedagógicos del curso.

**Base construida por el Ing. Javier Toquica.**
