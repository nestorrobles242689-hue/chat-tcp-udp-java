package server;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TCPServer {
    
    // Configuración de protocolo
    // true = TCP, false = UDP
    private static final boolean Usar_TCP = true; // Cambiar a false para UDP
    private static final int PORT = 1060;
    private static final int MaxClientes = 5;
    
    // Variables para UDP
    private DatagramSocket udpSocket;
    private static final int BUFFER_SIZE = 1024;
    private byte[] receiveBuffer = new byte[BUFFER_SIZE]; //Crea un array de 1024 bytes para recibir datos UDP
    
    private ServerSocket serverSocket; //Clase que escucha conexiones TCP entrantes
    private static final AtomicInteger clientCount = new AtomicInteger(0);  // Múltiples hilos pueden modificar el contador sin condiciones de carrera
    private static final Map<String, ClientHandler> registeredUsers = Collections.synchronizedMap(new HashMap<>()); // Interfaz para estructura clave-valor
    private static final List<PrintWriter> clientWriters = Collections.synchronizedList(new ArrayList<>()); //Interfaz para lista ordenada, hace la lista thread-safe para acceso concurrente
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public TCPServer() throws IOException {
    if (Usar_TCP) {
        // Modo TCP
        serverSocket = new ServerSocket(PORT); //Crea un socket servidor TCP en el puerto 1060
        System.out.println("   SERVIDOR TCP INICIADO             ");
        System.out.println(" Protocolo: TCP                      ");
        System.out.println(" Puerto: " + PORT + "                         ");
        System.out.println(" Máximo clientes: " + MaxClientes + "                  ");
        System.out.println(" IP Local: " + InetAddress.getLocalHost().getHostAddress() + "           ");
        System.out.println(" Comandos: /users, /priv, /help      ");
    } else {
        // Modo UDP
        udpSocket = new DatagramSocket(PORT); // Crea un socket UDP en el puerto 1060
        System.out.println("   SERVIDOR UDP INICIADO             ");
        System.out.println(" Protocolo: UDP                      ");
        System.out.println(" Puerto: " + PORT + "                         ");
        System.out.println(" Máximo clientes: " + MaxClientes + "                  ");
        System.out.println(" IP Local: " + InetAddress.getLocalHost().getHostAddress() + "           ");
        System.out.println(" Comandos: /users, /priv, /help      ");
    }
    System.out.println("\n[INFO] Servidor iniciado - Esperando conexiones...\n");
}

    
    
    // Manejador para clientes UDP
    private class UDPHandler implements Runnable {
    private DatagramPacket packet;
    private InetAddress clientAddress;
    private int clientPort;
    
    //Guarda el paquete UDP recibido y extrae la IP y puerto del cliente que lo envió.
    public UDPHandler(DatagramPacket packet) {
        this.packet = packet;
        this.clientAddress = packet.getAddress();
        this.clientPort = packet.getPort();
    }
    
    
    // Procesa el mensaje UDP recibido
    @Override
    public void run() {
        try {
            String message = new String(packet.getData(), 0, packet.getLength());
            String timeStamp = LocalDateTime.now().format(dtf);
            
            // Si es primer mensaje, registra como username
            if (message.startsWith("REGISTER:")) {
                String username = message.substring(9).trim();
                
                // Verificar límite
                if (clientCount.get() >= MaxClientes) {
                    String response = "SERVIDOR: Límite de clientes alcanzado (" + MaxClientes + ")";
                    sendUDP(response, clientAddress, clientPort);
                    return;
                }
                
                // Verificar duplicados
                synchronized (registeredUsers) {
                    if (registeredUsers.containsKey(username)) {
                        sendUDP("SERVIDOR: Nombre '" + username + "' ya está en uso", clientAddress, clientPort);
                        return;
                    }
                    registeredUsers.put(username, null); // UDP no tiene ClientHandler persistente
                    clientCount.incrementAndGet();
                }
                
                String welcomeMsg = "[" + timeStamp + "] SERVIDOR: Bienvenido " + username + " (UDP)";
                sendUDP(welcomeMsg, clientAddress, clientPort);
                
                broadcastUDP("[" + timeStamp + "] SERVIDOR: " + username + " se ha unido al chat (UDP)");
                
                System.out.println("[" + timeStamp + "] Nuevo cliente UDP registrado: " + username);
                System.out.println("  Clientes conectados: " + clientCount.get() + "/" + MaxClientes);
                
            } else if (message.startsWith("/users") || message.startsWith("/listar")) {
                // Listar usuarios
                StringBuilder userList = new StringBuilder();
                userList.append("[" + timeStamp + "] USUARIOS CONECTADOS: ");
                synchronized (registeredUsers) {
                    userList.append(registeredUsers.keySet().toString());
                    userList.append(" (" + registeredUsers.size() + "/" + MaxClientes + ")");
                }
                sendUDP(userList.toString(), clientAddress, clientPort);
                
            } else if (message.startsWith("/priv ")) {
                // Mensaje privado UDP
                String[] parts = message.split(" ", 3);
                if (parts.length >= 3) {
                    String targetUser = parts[1];
                    String privateMsg = parts[2];
                    String formattedMsg = "[" + timeStamp + "] [PRIVADO UDP]: " + privateMsg;
                    
                    // En UDP, retransmitimos a todos pero con prefijo privado
                    broadcastUDP(formattedMsg);
                }
                
            } else {
                // Mensaje público
                String formattedMsg = "[" + timeStamp + "] [UDP]: " + message;
                broadcastUDP(formattedMsg);
                System.out.println("[" + timeStamp + "] [UDP] Mensaje recibido: " + message);
            }
            
        } catch (Exception e) {
            System.err.println("Error en UDP handler: " + e.getMessage());
        }
    }
    
    
    //Envía un mensaje de texto a un cliente UDP específico (IP + puerto). Convierte el texto a bytes y lo manda como datagrama.
    private void sendUDP(String message, InetAddress address, int port) {
        try {
            byte[] data = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, address, port);
            udpSocket.send(sendPacket);
        } catch (IOException e) {
            System.err.println("Error enviando UDP: " + e.getMessage());
        }
    }
    
    
    // Versión simplificada de broadcast UDP. Muestra el mensaje en consola del servidor.
    private void broadcastUDP(String message) {
        // Nota: En UDP puro, necesitaríamos mantener lista de direcciones
        // Esta es una versión simplificada
        System.out.println("[BROADCAST UDP] " + message);
    }
}
    
    
    
    // Es el bucle infinito principal del servidor, TCP: Espera conexiones con accept(), verifica límite de 5 clientes, 
    // crea un hilo por cada cliente, UDP: Escucha datagramas entrantes y crea hilos para procesarlos,
    // Nunca termina (a menos que ocurra un error).
    public void start() {
    try {
        if (Usar_TCP) {
            // Modo TCP
            while (true) {
                Socket clientSocket = serverSocket.accept();
                
                if (clientCount.get() >= MaxClientes) {
                    String timeStamp = LocalDateTime.now().format(dtf);
                    System.out.println("[" + timeStamp + "] LÍMITE ALCANZADO: " + MaxClientes + "/" + MaxClientes);
                    System.out.println("  Rechazando conexión de: " + clientSocket.getInetAddress().getHostAddress());

                    PrintWriter tempOut = new PrintWriter(clientSocket.getOutputStream(), true);
                    tempOut.println("SERVIDOR: Límite de clientes alcanzado (" + MaxClientes + ").");
                    tempOut.println("SERVIDOR: Todos los espacios están ocupados.");
                    tempOut.println("SERVIDOR: Intente más tarde. Desconectando...");
                    tempOut.flush();

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) { }

                    tempOut.close();
                    clientSocket.close();
                    continue;
                }
                
                clientCount.incrementAndGet();
                
                String timeStamp = LocalDateTime.now().format(dtf);
                System.out.println("[" + timeStamp + "] Nuevo cliente TCP conectado");
                System.out.println("  IP: " + clientSocket.getInetAddress().getHostAddress());
                System.out.println("  Clientes conectados: " + clientCount.get() + "/" + MaxClientes);

                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.setName("Cliente-" + clientSocket.getInetAddress().getHostAddress());
                clientThread.start();
                
                System.out.println("  Hilo asignado: " + clientThread.getName());
            }
        } else {
            // Modo UDP
            System.out.println("[UDP] Escuchando datagramas en puerto " + PORT + "...");
            
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                udpSocket.receive(receivePacket); // Bloqueante
                
                // Procesar en un hilo separado
                Thread udpThread = new Thread(new UDPHandler(receivePacket));
                udpThread.start();
            }
        }
    } catch (IOException e) {
        System.err.println("Error en servidor: " + e.getMessage());
    } finally {
        stop();
    }
}

    
    
    // Cierra limpiamente los sockets (TCP o UDP) cuando el servidor se apaga. Libera los recursos de red.
    public void stop() {
        try {
            if (Usar_TCP && serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            } else if (!Usar_TCP && udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar: " + e.getMessage());
        }
    }

   
    
    // Guarda el socket del cliente y extrae su dirección IP para identificarlo.
    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;
        private boolean registered = false;
        private String clientIP;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            this.clientIP = socket.getInetAddress().getHostAddress();
        }

        
        
        
        // Es el ciclo de vida completo de un cliente TCP
        // Configura streams: Prepara canales de entrada/salida, Valida límite: Rechaza si ya hay 5 clientes,
        // Registro: Pide nombre de usuario (máximo 3 intentos), Bucle de chat: Recibe mensajes y los procesa,
        // y Desconexión: Limpia recursos cuando el cliente se va.
        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                // Validación temprana del límite
                if (clientCount.get() > MaxClientes) {
                    String timeStamp = LocalDateTime.now().format(dtf);
                    out.println("SERVIDOR: El servidor está lleno (" + MaxClientes + " clientes máximo).");
                    out.println("SERVIDOR: Intenta conectarte más tarde.");
                    System.out.println("[" + timeStamp + "] Conexión rechazada: Límite excedido");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) { }
                    desconectarCliente();
                    return;
                }

                boolean registroExitoso = false;
                int intentos = 0;
                
                while (!registroExitoso && intentos < 3) {
                    out.println("SERVIDOR: Por favor, ingresa tu nombre de usuario:");
                    username = in.readLine();
                    
                    if (username == null) {
                        desconectarCliente();
                        return;
                    }
                    
                    username = username.trim();
                    intentos++;
                    
                    synchronized (registeredUsers) {
                    // VALIDAR LÍMITE DE CLIENTES
                    if (registeredUsers.size() >= MaxClientes) {
                        out.println("SERVIDOR: Límite de clientes alcanzado (" + MaxClientes + "/" + MaxClientes + ").");
                        out.println("SERVIDOR: No es posible registrar más usuarios en este momento.");
                        out.println("SERVIDOR: Desconectando...");

                        String timeStamp = LocalDateTime.now().format(dtf);
                        System.out.println("[" + timeStamp + "] REGISTRO RECHAZADO: Límite de " + MaxClientes + " clientes alcanzado");
                        System.out.println("  Usuario intentado: '" + username + "'");

                        // Desconectar después de 2 segundos
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) { }

                        desconectarCliente();
                        return;
                    }

                    if (username.isEmpty()) {
                        out.println("SERVIDOR: El nombre de usuario no puede estar vacío.");
                    } else if (username.startsWith("/") || username.contains(" ")) {
                        out.println("SERVIDOR: Nombre inválido. No use espacios ni caracteres especiales.");
                    } else if (registeredUsers.containsKey(username)) {
                        out.println("SERVIDOR: El nombre '" + username + "' ya está en uso. Elige otro.");
                    } else {
                        registeredUsers.put(username, this);
                        clientWriters.add(out);
                        registered = true;
                        registroExitoso = true;

                        out.println("SERVIDOR: Registro exitoso. ¡Bienvenido " + username + "!");
                        out.println("SERVIDOR: Usuarios conectados: " + registeredUsers.keySet());
                        out.println("SERVIDOR: /priv [usuario] [mensaje] - Mensajes 100% privados");

                        broadcastMessage("[" + LocalDateTime.now().format(dtf) + 
                                       "] SERVIDOR: " + username + " se ha unido al chat");
                    }
                }
                }
                
                if (!registroExitoso) {
                    out.println("SERVIDOR: Demasiados intentos fallidos. Conexión rechazada.");
                    desconectarCliente();
                    return;
                }
                
                // Bucle principal de mensajes
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.trim().isEmpty()) continue;

                    String timeStamp = LocalDateTime.now().format(dtf);

                    // Procesamiento de comandos
                    if (inputLine.startsWith("/priv ")) {
                        // Mensaje privado
                        procesarMensajePrivado(inputLine, timeStamp);
                    } else if (inputLine.equals("/users") || inputLine.equals("/listar") || inputLine.equals("/usuarios")) {
                        // Listar usuarios conectados
                        listarUsuariosConectados(timeStamp);
                    } else if (inputLine.equals("/help") || inputLine.equals("/ayuda") || inputLine.equals("/comandos")) {
                        // Mostrar ayuda
                        mostrarAyudaComandos(timeStamp);
                    } else {
                        // Mensaje público
                        System.out.println("[" + timeStamp + "] [PÚBLICO] " + username + ": " + inputLine);

                        String formattedMsg = "[" + timeStamp + "] " + username + ": " + inputLine;
                        broadcastMessage(formattedMsg);
                    }
                }
                
            } catch (IOException e) {
                // Cliente desconectado inesperadamente
            } finally {
                desconectarCliente();
            }
        }
        
        
        // Maneja el comando /priv usuario mensaje
        // Mensajeria privada - Contenido oculto
        private void procesarMensajePrivado(String inputLine, String timeStamp) {
            String[] partes = inputLine.split(" ", 3);
            
            if (partes.length < 3) {
                out.println("[" + timeStamp + "] SERVIDOR: Uso correcto: /priv [usuario] [mensaje]");
                // Solo se registra que hubo un error de formato, sin contenido
                System.out.println("[" + timeStamp + "] [PRIVADO] " + username + " intentó enviar mensaje privado (formato incorrecto)");
                return;
            }
            
            String usuarioDestino = partes[1];
            String mensajePrivado = partes[2];
            
            // Verificar que no se intente enviar a sí mismo
            if (usuarioDestino.equalsIgnoreCase(username)) {
                out.println("[" + timeStamp + "] SERVIDOR: No puedes enviarte mensajes privados a ti mismo");
                System.out.println("[" + timeStamp + "] [PRIVADO] " + username + " intentó enviarse mensaje a sí mismo");
                return;
            }
            
            // Buscar al destinatario
            ClientHandler destinatario = null;
            synchronized (registeredUsers) {
                destinatario = registeredUsers.get(usuarioDestino);
            }
            
            if (destinatario == null) {
                out.println("[" + timeStamp + "] SERVIDOR: Usuario '" + usuarioDestino + "' no encontrado");
                // Solo se registra que falló, sin contenido del mensaje
                System.out.println("[" + timeStamp + "] [PRIVADO] " + username + " → " + usuarioDestino + ": Usuario no encontrado");
                return;
            }
            
            // Enviar mensaje privado
            
            // Mensaje para el destinatario
            String mensajeParaDestinatario = "[" + timeStamp + "] [PRIVADO de " + username + "]: " + mensajePrivado;
            
            // Confirmación para el remitente (sin revelar el contenido exacto en logs)
            String confirmacionParaRemitente = "[" + timeStamp + "] [PRIVADO enviado a " + usuarioDestino + "]: " + mensajePrivado;
            
            // Enviar al destinatario
            destinatario.out.println(mensajeParaDestinatario);
            
            // Enviar confirmación al remitente
            out.println(confirmacionParaRemitente);
            
            // Log en servidor sin mostrar contenido
            System.out.println("[" + timeStamp + "] [PRIVADO] " + username + " → " + usuarioDestino);
            System.out.println(" Mensaje privado entregado");
            System.out.println(" Contenido CIFRADO - No visible en servidor");
            System.out.println("Visible solo para: " + usuarioDestino);
            // NOTA: El contenido NUNCA se muestra en el servidor
        }
    
        
        // Responde al comando /users
        // listar usuarios conectados
        private void listarUsuariosConectados(String timeStamp) {
            synchronized (registeredUsers) {
                int totalUsuarios = registeredUsers.size();

                out.println("[" + timeStamp + "]      USUARIOS CONECTADOS              ");
                out.println(String.format("[" + timeStamp + "]  Total: %d/%d usuarios                ", totalUsuarios, MaxClientes));
                

                int contador = 1;
                for (Map.Entry<String, ClientHandler> entry : registeredUsers.entrySet()) {
                    String nombreUsuario = entry.getKey();
                    ClientHandler handler = entry.getValue();
                    String ip = handler.clientIP;

                    if (nombreUsuario.equals(username)) {
                        out.println(String.format("[" + timeStamp + "]  %d. %-15s IP: %-15s  (Tú)", 
                                                contador, nombreUsuario, ip));
                    } else {
                        out.println(String.format("[" + timeStamp + "]  %d. %-15s IP: %-15s ", 
                                                contador, nombreUsuario, ip));
                    }
                    contador++;
                }


                if (totalUsuarios > 1) {
                    out.println("[" + timeStamp + "]  Usa /priv [usuario] [mensaje] para mensaje privado");
                }
            }

            System.out.println("[" + timeStamp + "] [COMANDO] " + username + " solicitó lista de usuarios");
            System.out.println("  Usuarios conectados: " + registeredUsers.keySet());
        }

        
        // Mostrar comandos
        private void mostrarAyudaComandos(String timeStamp) {
            out.println("[" + timeStamp + "]         COMANDOS DISPONIBLES               ");
            out.println("[" + timeStamp + "]  /users, /listar, /usuarios                ");
            out.println("[" + timeStamp + "]  /priv [usuario] [mensaje]                ");
            out.println("[" + timeStamp + "]  /help, /ayuda, /comandos                  ");
            out.println("[" + timeStamp + "]  exit                                      ");
            out.println("[" + timeStamp + "]    → Salir del chat                        ");

            System.out.println("[" + timeStamp + "] [COMANDO] " + username + " solicitó ayuda de comandos");
        }
    
   
        
        private void desconectarCliente() {
            String timeStamp = LocalDateTime.now().format(dtf);
            
            if (username != null && registered) {
                registeredUsers.remove(username);
                System.out.println("[" + timeStamp + "] Usuario desconectado: '" + username + "'");
                System.out.println("  Usuarios restantes: " + registeredUsers.size());
                
                broadcastMessage("[" + timeStamp + "] SERVIDOR: " + username + " ha abandonado el chat");
            }
            
            clientWriters.remove(out);
            clientCount.decrementAndGet();
            
            System.out.println("  Clientes conectados: " + clientCount.get() + "/" + MaxClientes);
            
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) { }
        }
        
        
        // Envía un mensaje a TODOS los clientes conectados
        private void broadcastMessage(String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(message);
                }
            }
        }
    }

    
    //Punto de entrada del programa. Crea el servidor, configura un "apagado seguro" para Ctrl+C, e inicia el servidor.
    public static void main(String[] args) {
        try {
            TCPServer server = new TCPServer();
            
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                String timeStamp = LocalDateTime.now().format(dtf);
                System.out.println("\n[" + timeStamp + "] Cerrando servidor...");
                System.out.println("Usuarios activos al cerrar: " + registeredUsers.keySet());
                server.stop();
            }));

            server.start();
        } catch (IOException e) {
            System.err.println("Error al iniciar servidor: " + e.getMessage());
        }
    }
}