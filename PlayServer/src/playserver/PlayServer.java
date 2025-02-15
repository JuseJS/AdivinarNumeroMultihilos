package playserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servidor para el juego de adivinar números Los jugadores se conectan y por
 * turnos intentan adivinar un número secreto
 */
public class PlayServer {

    // Número secreto que los jugadores deben adivinar
    private static int secretNumber;
    // Número total de jugadores necesarios para comenzar
    private static int numPlayers = 2;
    // Indica si el juego ha terminado (thread-safe)
    private static AtomicBoolean ended = new AtomicBoolean(false);
    // Indica el turno actual (thread-safe)
    private static AtomicInteger turn = new AtomicInteger(0);
    // Objeto para sincronización entre hilos
    private static Object lock = new Object();
    // Lista de todos los hilos de jugadores
    private static List<ServerThread> players = new ArrayList<>();
    // Indica si el juego ha comenzado
    private static AtomicBoolean gameStarted = new AtomicBoolean(false);

    public static void main(String[] args) {
        int serverPort = 5000;

        try {
            // Inicializar el servidor
            ServerSocket serverSocket = new ServerSocket(serverPort);
            System.out.println("Servidor de juego iniciado en el puerto " + serverPort + ".");

            // Generar número aleatorio entre 0 y 100
            secretNumber = new Random().nextInt(101);
            System.out.println("El número secreto es " + secretNumber + ".");
            System.out.println("Esperando a que se conecten " + numPlayers + " jugadores.");

            // Esperar conexiones de jugadores
            for (int i = 0; i < numPlayers; i++) {
                Socket clientSocket = serverSocket.accept();
                System.out.printf("Jugador %d conectado desde %s.\n", i, clientSocket.getInetAddress().getHostAddress());

                ServerThread serverThread = new ServerThread(clientSocket, i);
                players.add(serverThread);
                new Thread(serverThread).start();
            }

            // Iniciar el juego cuando todos están conectados
            gameStarted.set(true);
            synchronized (lock) {
                lock.notifyAll();
            }

            // Notificar a todos los jugadores que el juego ha comenzado
            for (ServerThread player : players) {
                player.notifyGameStart();
            }

        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }

    /**
     * Clase interna que maneja la conexión con cada jugador
     */
    static class ServerThread implements Runnable {

        private Socket socket;
        private BufferedReader input;
        private PrintWriter output;
        private int playerTurn;

        public ServerThread(Socket socket, int playerTurn) {
            this.socket = socket;
            this.playerTurn = playerTurn;
        }

        /**
         * Notifica al jugador que el juego ha comenzado
         */
        public void notifyGameStart() {
            output.println("¡Todos los jugadores están conectados! El juego comienza.");
        }

        /**
         * Notifica a los jugadores quién ha ganado
         */
        public void notifyWinner(int winner) {
            output.println("¡El jugador " + winner + " ha ganado! El número secreto era " + secretNumber);
        }

        @Override
        public void run() {
            try {
                // Configurar streams de entrada/salida
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);

                // Enviar mensaje de bienvenida
                output.println("Bienvenido al juego 'Adivina el número!' Eres el jugador nº " + this.playerTurn);
                output.println("Esperando a los demás jugadores...");

                // Esperar a que el juego comience
                while (!gameStarted.get()) {
                    synchronized (lock) {
                        lock.wait(100);
                    }
                }

                // Bucle principal del juego
                while (!ended.get()) {
                    synchronized (lock) {
                        // Esperar mientras no sea el turno del jugador
                        while (playerTurn != turn.get() && !ended.get()) {
                            output.println("Esperando el turno del jugador " + turn.get());
                            lock.wait();
                        }

                        if (ended.get()) {
                            break;
                        }

                        // Procesar el turno del jugador
                        output.println("Tu turno. Introduce un número (0-100): ");
                        String inputLine = input.readLine();

                        if (inputLine == null) {
                            break;
                        }

                        try {
                            int attempt = Integer.parseInt(inputLine);

                            // Verificar si el número es correcto
                            if (attempt == secretNumber) {
                                output.println("¡Has acertado! ¡Has ganado!");
                                ended.set(true);
                                // Notificar a los demás jugadores
                                for (ServerThread player : players) {
                                    if (player != this) {
                                        player.notifyWinner(playerTurn);
                                    }
                                }
                            } else if (attempt < secretNumber) {
                                output.println("El número secreto es mayor.");
                            } else {
                                output.println("El número secreto es menor.");
                            }

                            // Pasar al siguiente turno
                            turn.set((turn.get() + 1) % numPlayers);
                            lock.notifyAll();
                        } catch (NumberFormatException e) {
                            output.println("Por favor, introduce un número válido.");
                        }
                    }
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("Error en la conexión con el jugador " + playerTurn + ": " + e.getMessage());
            } finally {
                // Cerrar la conexión
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Error al cerrar la conexión: " + e.getMessage());
                }
            }
        }
    }
}
