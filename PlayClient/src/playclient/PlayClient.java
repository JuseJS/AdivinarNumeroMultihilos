package playclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

public class PlayClient {

    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private Random random;

    // Variables para el rango de números
    private int min = 0;
    private int max = 100;
    private int ultimoIntento = -1;

    public PlayClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.random = new Random();
    }

    public void iniciar() {
        try {
            conectar();
            jugar();
        } catch (IOException e) {
            System.err.println("Error en el cliente: " + e.getMessage());
        } finally {
            cerrarConexion();
        }
    }

    private void conectar() throws IOException {
        socket = new Socket(host, port);
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        salida = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("Conectado al servidor " + host + ":" + port);
    }

    private void jugar() throws IOException {
        String mensajeServidor;

        while ((mensajeServidor = entrada.readLine()) != null) {
            System.out.println(mensajeServidor);

            // Si es nuestro turno
            if (mensajeServidor.contains("Tu turno")) {
                realizarIntento();
            }

            // Si alguien ha ganado
            if (mensajeServidor.contains("ha ganado")) {
                break;
            }

            // Procesar la respuesta del servidor para ajustar el rango
            if (mensajeServidor.contains("es mayor")) {
                min = ultimoIntento + 1;
            } else if (mensajeServidor.contains("es menor")) {
                max = ultimoIntento - 1;
            }
        }
    }

    private void realizarIntento() {
        System.out.println("\nRango actual: " + min + " - " + max);

        // Generar número aleatorio dentro del rango actual
        int intento;
        do {
            intento = random.nextInt(max - min + 1) + min;
        } while (intento == ultimoIntento); // Evitar repetir el último número

        System.out.println("Número generado: " + intento);
        ultimoIntento = intento;
        salida.println(intento);
    }

    private void cerrarConexion() {
        try {
            if (entrada != null) {
                entrada.close();
            }
            if (salida != null) {
                salida.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar la conexión: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Uso: java PlayClient <host> <puerto>");
            System.out.println("Ejemplo: java PlayClient localhost 5000");
            return;
        }

        String host = args[0];
        int puerto;

        try {
            puerto = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Error: El puerto debe ser un número válido");
            return;
        }

        PlayClient cliente = new PlayClient(host, puerto);
        cliente.iniciar();
    }
}
