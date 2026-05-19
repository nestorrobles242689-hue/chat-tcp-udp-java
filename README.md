# 💬 CHAT TCP/UDP EN JAVA
Aplicación de chat cliente-servidor desarrollada en Java que permite comunicación en tiempo real mediante sockets TCP y UDP, con soporte para mensajes grupales y privados entre múltiples clientes.

# 📋 Descripción
El sistema implementa una arquitectura cliente-servidor donde un servidor central gestiona las conexiones y retransmite mensajes entre los clientes conectados. Se utilizan hilos independientes para manejar cada cliente de forma concurrente, permitiendo que varios usuarios chatíen simultáneamente sin bloqueos.

# 👥 Integrantes
Nombre: Néstor David Robles Ortiz 	Matrícula: 00000242689

# 🛠️ Tecnologías utilizadas

    Java 25+
    TCP Sockets — conexión confiable orientada a flujo
    UDP Sockets — comunicación sin conexión de baja latencia
    Multihilos (Thread / Runnable) — manejo concurrente de clientes
    Maven / Apache NetBeans

# Funcionalidad Extra:
Uso de emojis por medio de cambiar cierto orden de carácteres por dichos emojis. Ejemplo: 
private static String convertirEmojis(String mensaje) {
        // Reemplazar atajos comunes por emojis Unicode
        mensaje = mensaje.replace(":)", "😊");


# 📁 Estructura del proyecto
<img width="545" height="325" alt="imagen" src="https://github.com/user-attachments/assets/7b41fac8-3b7a-4962-ba39-4404572063e0" />


# 🚀 Cómo ejecutar
Requisitos previos

    Java JDK 25 o superior instalado
    Terminal / Git Bash

# Compilar el proyecto
javac -d bin src/**/*.java

# Iniciar el servidor
java -cp bin server.TCPServer
El servidor escuchará en el puerto 1060 (TCP) y 1060 (UDP) por defecto.

# Conectar un cliente
java -cp bin client.TCPClient
Se te pedirá ingresar nombre de usuario.

# ⚙️ Funcionalidades

    Registro de usuario con nombre único
    Mensajes grupales visibles para todos los conectados
    Mensajes privados entre usuarios (/priv <usuario> <mensaje>)
    Lista de usuarios conectados (/users, /listar)
    Desconexión controlada (/Exit)
    Funcionalidad extra: Uso de Emojis

# 📸 Capturas de pantalla
# Servidor activo
<img width="1883" height="1118" alt="imagen" src="https://github.com/user-attachments/assets/20d82328-7c03-472c-b38e-f43743a2e0f5" />

# Registro de usuario
<img width="1913" height="1123" alt="imagen" src="https://github.com/user-attachments/assets/5c358a96-6509-40c6-baab-8a148b77e60a" />
<img width="1902" height="1116" alt="imagen" src="https://github.com/user-attachments/assets/edd0d287-df9c-499b-8c5b-d07e76b988ed" />


# Chat grupal
<img width="1919" height="1125" alt="imagen" src="https://github.com/user-attachments/assets/7d6a0317-925a-4f85-90f9-b38c933efc0f" />


# Mensaje privado
<img width="1913" height="1116" alt="imagen" src="https://github.com/user-attachments/assets/00c01e4d-ddbe-4f06-99cc-eb08969767b4" />
<img width="1916" height="1125" alt="imagen" src="https://github.com/user-attachments/assets/f00a113a-2694-400a-b11b-7734a7ddfe22" />



# 🌐 Protocolo de comunicación
<img width="406" height="150" alt="imagen" src="https://github.com/user-attachments/assets/929137bd-9e21-4049-907c-6378858064f9" />


# 📄 Licencia
Proyecto académico — ITSON · Redes · 2026
