package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;


//Inicializa la conexión con el servidor
public class TCPClient {
    // true = TCP, false = UDP
    private static final boolean USE_TCP = true; // Cambiar a false para UDP
    
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 1060;
    private static final int BUFFER_SIZE = 1024; //Tamaño del buffer para recibir datagramas UDP
    
    // Variables TCP
    private Socket socket; // Clase para comunicación UDP (no orientada a conexión)
    private BufferedReader in; 
    private PrintWriter out;
    
    // Variables UDP
    private DatagramSocket udpSocket;
    private InetAddress serverAddress;
    
    private Thread receiverThread;
    private boolean connected = false;
    
    public static String usernameIn;

    public TCPClient() throws IOException {
        if (USE_TCP) {
            // Modo TCP
            System.out.println("   CLIENTE TCP CHAT + PRIVADOS       ");
            System.out.println(" Conectando TCP a: " + SERVER_IP + ":" + SERVER_PORT + "     ");
            
            socket = new Socket(SERVER_IP, SERVER_PORT);
            connected = true;

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println(" Conectado al servidor TCP");
            
        } else {
            // Modo UDP
            System.out.println("   CLIENTE UDP CHAT                  ");
            System.out.println(" Conectando UDP a: " + SERVER_IP + ":" + SERVER_PORT + "     ");
            
            udpSocket = new DatagramSocket();
            serverAddress = InetAddress.getByName(SERVER_IP);
            connected = true;
            
            System.out.println(" Cliente UDP listo en puerto: " + udpSocket.getLocalPort());
        }
        
        System.out.println("IP local: " + (USE_TCP ? socket.getLocalAddress().getHostAddress() : InetAddress.getLocalHost().getHostAddress()));
        System.out.println();
        
        // Solicitar nombre de usuario
        Scanner scanner = new Scanner(System.in);
        System.out.print("Ingresa tu nombre de usuario: ");
        usernameIn = scanner.nextLine().trim();
        
        // Registrar con el servidor
        if (USE_TCP) {
            out.println(usernameIn);
        } else {
            sendUDPMessage("REGISTER:" + usernameIn);
        }
        
        System.out.println("Esperando validación del servidor...");
    }

    
    //Crea un hilo separado para recibir mensajes
    public void startReceiving() {
        receiverThread = new Thread(() -> {
            try {
                if (USE_TCP) {
                    // Recepción UDP
                    String response;
                    boolean esperandoRegistro = true;
                    
                    while (connected && (response = in.readLine()) != null) {
                        if (response.contains("Ingresa tu nombre de usuario")) {
                            System.out.println(response);
                            System.out.print("> ");
                        } else if (response.contains("ya está en uso")) {
                            System.out.println(response);
                            System.out.print("Nuevo nombre de usuario: ");
                            Scanner scanner = new Scanner(System.in);
                            usernameIn = scanner.nextLine().trim();
                            out.println(usernameIn);
                        } else if (response.contains("Registro exitoso")) {
                            System.out.println(response);
                            esperandoRegistro = false;
                            System.out.println("\n¡Ya puedes chatear!");
                            System.out.print("> ");
                        } else if (!esperandoRegistro) {
                            System.out.print("\r" + response);
                            System.out.print("\n> ");
                        } else if (response.contains("Límite de clientes alcanzado") || response.contains("El servidor está lleno")) {
                             System.out.println("\n" + response);
                             System.out.println(" No se pudo conectar. El chat está lleno.");
                             connected = false;
                             break;
                         } else {
                             System.out.println(response);
                         }
                    }
                } else {
                    // Recepción UDP
                    byte[] buffer = new byte[BUFFER_SIZE];
                    
                    while (connected) {
                        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                        udpSocket.receive(receivePacket);
                        
                        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        System.out.print("\r" + response);
                        System.out.print("\n> ");
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("\n Conexión perdida con el servidor.");
                }
            }
        });
        receiverThread.start();
    }

    
    //Envía un mensaje al servidor
    public void sendMessage(String message) {
        if (!connected) return;

        // Convertir atajos de emojis antes de enviar
        String mensajeConEmojis = convertirEmojis(message);

        try {
            if (USE_TCP) {
                // Envío TCP
                if (out != null) {
                    out.println(mensajeConEmojis);
                }
            } else {
                // Envío UDP
                sendUDPMessage(mensajeConEmojis);
            }
        } catch (Exception e) {
            System.err.println("Error enviando mensaje: " + e.getMessage());
        }
    }
    
    
    //Envía un mensaje como datagrama UDP
    private void sendUDPMessage(String message) throws IOException {
        byte[] data = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
        udpSocket.send(sendPacket);
    }

    
    //Desconecta al cliente limpiamente
    public void stop() {
        connected = false;
        try {
            if (USE_TCP) {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } else {
                if (udpSocket != null && !udpSocket.isClosed()) {
                    udpSocket.close();
                }
            }
            if (receiverThread != null) {
                receiverThread.join(1000);
            }
        } catch (Exception e) {
            System.err.println("Error al cerrar: " + e.getMessage());
        }
        System.out.println("Desconectado del chat.");
    }

    
    // Punto de entrada del cliente
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        TCPClient client = null;

        try {
            client = new TCPClient();
            client.startReceiving();

            Thread.sleep(1000);
            
            System.out.println("\n");
            System.out.println("  COMANDOS DISPONIBLES (" + (USE_TCP ? "TCP" : "UDP") + "):");
            System.out.println("  /users, /listar - Ver usuarios");
            System.out.println("  /priv [user] [msg] - Mensaje privado");
            System.out.println("  /help - Ayuda de comandos");
            System.out.println("  exit - Salir");
            System.out.println("\n");

            while (client.connected) {
                System.out.print("> ");
                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("exit")) {
                    client.sendMessage("Saliendo...");
                    client.stop();
                    break;
                } else if (!input.isEmpty()) {
                    client.sendMessage(input);
                }
            }

        } catch (IOException e) {
            System.err.println("Error de conexión: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            if (client != null) client.stop();
            scanner.close();
        }
    }
    
   

    // Método para convertir atajos de texto a emojis
    private static String convertirEmojis(String mensaje) {
        // Reemplazar atajos comunes por emojis Unicode
        mensaje = mensaje.replace(":)", "😊");
        mensaje = mensaje.replace(":D", "😃");
        mensaje = mensaje.replace(":(", "😢");
        mensaje = mensaje.replace(":P", "😛");
        mensaje = mensaje.replace(";)", "😉");
        mensaje = mensaje.replace("<3", "❤️");
        mensaje = mensaje.replace(":O", "😮");
        mensaje = mensaje.replace(":'(", "😢");
        mensaje = mensaje.replace(":S", "😕");
        mensaje = mensaje.replace(":|", "😐");
        mensaje = mensaje.replace(":B", "😬");
        mensaje = mensaje.replace(":3", "😺");
        mensaje = mensaje.replace("(y)", "👍");
        mensaje = mensaje.replace("(n)", "👎");
        mensaje = mensaje.replace("(ok)", "👌");
        mensaje = mensaje.replace("(f)", "🌸");
        mensaje = mensaje.replace("(s)", "⭐");
        mensaje = mensaje.replace("(m)", "🎵");
        mensaje = mensaje.replace("(c)", "☕");
        mensaje = mensaje.replace("(p)", "🍕");
        mensaje = mensaje.replace("(t)", "🌮");
        mensaje = mensaje.replace("(h)", "❤️");
        mensaje = mensaje.replace("(b)", "🎂");
        mensaje = mensaje.replace("(g)", "🎁");
        mensaje = mensaje.replace("(z)", "💤");
        mensaje = mensaje.replace("(w)", "🌊");
        mensaje = mensaje.replace("(r)", "🌈");
        mensaje = mensaje.replace("(l)", "🔒");
        mensaje = mensaje.replace("(k)", "🔑");
        mensaje = mensaje.replace("(e)", "📧");
        mensaje = mensaje.replace("(t)", "📞");
        mensaje = mensaje.replace("(i)", "💡");
        mensaje = mensaje.replace("(!!)", "⚠️");
        mensaje = mensaje.replace("(?)", "❓");
        mensaje = mensaje.replace("(x)", "❌");
        mensaje = mensaje.replace("(+)", "✅");

        return mensaje;
    }

    
    
}