package playclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class PlayClient {

    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private Scanner scanner;

    public PlayClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.scanner = new Scanner(System.in);
    }

    public void iniciar() {
        try {
            conectar();
            jugar();
        } catch (IOException e) {
            System.out.println("\n❌ Error en el cliente: " + e.getMessage());
        } finally {
            cerrarConexion();
        }
    }

    private void conectar() throws IOException {
        socket = new Socket(host, port);
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        salida = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("\n🔌 Conectado al servidor " + host + ":" + port);
    }

    private void jugar() throws IOException {
        String mensajeServidor;
        boolean esperandoIntento = false;

        while ((mensajeServidor = entrada.readLine()) != null) {
            // Mostrar el mensaje del servidor
            System.out.println(formatearMensaje(mensajeServidor));

            // Si el mensaje pide un número y no estamos ya esperando uno
            if (mensajeServidor.contains("Introduce un número") && !esperandoIntento) {
                esperandoIntento = true;
                realizarIntento();
                esperandoIntento = false;
            }

            // Si el juego ha terminado
            if (mensajeServidor.contains("¡Felicidades! ¡Has adivinado el número secreto!")) {
                break;
            }
        }
    }

    private String formatearMensaje(String mensaje) {
        if (mensaje.contains("Bienvenido")) {
            return "\n🎮 " + mensaje;
        } else if (mensaje.contains("Introduce un número")) {
            return "\n👉 " + mensaje;
        } else if (mensaje.contains("número secreto es mayor")) {
            return "📈 " + mensaje;
        } else if (mensaje.contains("número secreto es menor")) {
            return "📉 " + mensaje;
        } else if (mensaje.contains("¡Felicidades!")) {
            return "\n🎉 " + mensaje;
        }
        return mensaje;
    }

    private void realizarIntento() {
        while (true) {
            System.out.print("Introduce tu intento (0-100): ");
            String input = scanner.nextLine().trim();

            try {
                int intento = Integer.parseInt(input);
                if (intento >= 0 && intento <= 100) {
                    salida.println(intento);
                    return;
                } else {
                    System.out.println("❌ Por favor, introduce un número entre 0 y 100");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Por favor, introduce un número válido");
            }
        }
    }

    private void cerrarConexion() {
        try {
            if (scanner != null) {
                scanner.close();
            }
            if (entrada != null) {
                entrada.close();
            }
            if (salida != null) {
                salida.close();
            }
            if (socket != null) {
                socket.close();
            }
            System.out.println("\n👋 Conexión cerrada");
        } catch (IOException e) {
            System.out.println("❌ Error al cerrar la conexión: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("\n❌ Uso: java PlayClient <host> <puerto>");
            System.out.println("📝 Ejemplo: java PlayClient localhost 5000");
            return;
        }

        String host = args[0];
        int puerto;
        try {
            puerto = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("❌ Error: El puerto debe ser un número válido");
            return;
        }

        PlayClient cliente = new PlayClient(host, puerto);
        cliente.iniciar();
    }
}
