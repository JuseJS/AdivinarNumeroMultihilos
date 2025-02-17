package playserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.ConsoleHandler;
import java.util.logging.SimpleFormatter;

/**
 * Servidor para el juego de adivinar números.
 *
 * Cada jugador tiene su propia partida privada con un número secreto único
 */
public class PlayServer {

    private static final Logger LOGGER = Logger.getLogger(PlayServer.class.getName());
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        setupLogger();

        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            LOGGER.info("Servidor de juego iniciado en el puerto " + SERVER_PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                LOGGER.info("Nuevo jugador conectado desde " + clientSocket.getInetAddress().getHostAddress());

                // Crear una nueva partida para cada jugador
                GameInstance gameInstance = new GameInstance(clientSocket);
                new Thread(gameInstance).start();
            }
        } catch (IOException e) {
            LOGGER.severe("Error en el servidor: " + e.getMessage());
        }
    }

    private static void setupLogger() {
        // Configurar el logger para mostrar solo en consola
        LOGGER.setLevel(Level.ALL);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter());
        // Remover handlers por defecto y añadir solo el de consola
        LOGGER.setUseParentHandlers(false);
        LOGGER.addHandler(handler);
    }

    /**
     * Clase que representa una instancia de juego individual para cada jugador
     */
    static class GameInstance implements Runnable {

        private final Socket socket;
        private final int secretNumber;
        private final BufferedReader input;
        private final PrintWriter output;
        private static final Logger GAME_LOGGER = Logger.getLogger(GameInstance.class.getName());

        public GameInstance(Socket socket) throws IOException {
            this.socket = socket;
            this.secretNumber = new Random().nextInt(101);
            this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.output = new PrintWriter(socket.getOutputStream(), true);

            GAME_LOGGER.info("Nueva partida creada con número secreto: " + secretNumber);
        }

        @Override
        public void run() {
            try {
                // Enviar mensaje de bienvenida
                output.println("¡Bienvenido al juego 'Adivina el número!'");
                output.println("Intenta adivinar un número entre 0 y 100.");

                // Bucle principal del juego
                boolean gameEnded = false;
                while (!gameEnded) {
                    output.println("Introduce un número (0-100): ");
                    String inputLine = input.readLine();

                    if (inputLine == null) {
                        break;
                    }

                    try {
                        int attempt = Integer.parseInt(inputLine);
                        GAME_LOGGER.info("Jugador desde " + socket.getInetAddress().getHostAddress()
                                + " intentó con el número: " + attempt);

                        if (attempt == secretNumber) {
                            output.println("¡Felicidades! ¡Has adivinado el número secreto!");
                            GAME_LOGGER.info("Jugador ha ganado la partida");
                            gameEnded = true;
                        } else if (attempt < secretNumber) {
                            output.println("El número secreto es mayor.");
                        } else {
                            output.println("El número secreto es menor.");
                        }
                    } catch (NumberFormatException e) {
                        output.println("Por favor, introduce un número válido.");
                        GAME_LOGGER.warning("Input inválido recibido: " + inputLine);
                    }
                }
            } catch (IOException e) {
                GAME_LOGGER.severe("Error en la conexión con el jugador: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                    GAME_LOGGER.info("Conexión cerrada con el jugador");
                } catch (IOException e) {
                    GAME_LOGGER.severe("Error al cerrar la conexión: " + e.getMessage());
                }
            }
        }
    }
}
