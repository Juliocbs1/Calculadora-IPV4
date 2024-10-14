package co.edu.unbosque.redes.modelo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.springframework.stereotype.Service;

@Service
public class CalculadoraIPV4 {

	private String ip;
	private String mascara;
	private String direccion_red;
	private String broadcast;
	private int numero_host;
	private String claseIP;
	private String tipoIP;
	private String parte_red_binaria;
	private String parte_host_binaria;
	private String rangoIPs;

	public boolean calcularRedInfo(String ip_llegada, int mascara_llegada) {


		this.ip = ip_llegada;
		this.mascara = obtenerMascara(mascara_llegada);

		// Convertimos IP a un arreglo de bytes por ejemplo la ip 192.168.1.1 en un arreglo seria [192,168,1,1]

		InetAddress ipAddress = null;
		try {
			ipAddress = InetAddress.getByName(ip);
		} catch (UnknownHostException e) {
			return false;
		}
		byte[] ipBytes = ipAddress.getAddress();

		// Calcular dirección de red y broadcast
		byte[] networkBytes = calcularDireccionRed(ipBytes, mascara_llegada);
		byte[] broadcastBytes = calcularDireccionBroadcast(ipBytes, mascara_llegada);

		// Convertir las direcciones a formato String
		this.direccion_red = bytesToIp(networkBytes);
		this.broadcast = bytesToIp(broadcastBytes);

		// Calcular el número de hosts posibles
		this.numero_host = (int) Math.pow(2, 32 - mascara_llegada) - 2;

		// Determinar la clase de la IP
		this.claseIP = determinarClaseIP(ipBytes);
		// La IP es publica o privada
		if(esIPPrivada(ipBytes)) {
			this.tipoIP = "Privada";
		}else{
			this.tipoIP = "Publica";
		}

		// Calcular la porción de red y hosts en binario
		generarBinarios(ipBytes, mascara_llegada);

		// Calcular el rango de IPs útiles
		this.rangoIPs = calcularRangoIPs(networkBytes, broadcastBytes);
		return true;
	}

	// Método para obtener la máscara de subred en formato decimal
	private String obtenerMascara(int prefijo) {
		/*
		 -0xFFFFFFFF es un número hexadecimal que representa 32 bits todos en "1"
		 el << realiza un desplazamiento a la izquierda por n posiciones
		 un ejemplo si el prefijo es 24 entonces 32-24=8
		 se corren 8 posciones a la izquierda dejando 0 en ese lugar
		 -Un punto a decir es que la variable mask es -256 en un caso en el que el prefijo es 24
		 ya que java usa enteros con signo de 32 bits por lo que cualquier número en el que el
		 bit más significativo  sea 1 será interpretado como negativo.
		*/
		int mask = 0xFFFFFFFF << (32 - prefijo);
		/*
			-El string format me genera una cadena de texto formateada en la cual
			indico con %d. que sea 4 numero de decimales separados por puntos
			- el >>> desplaza los bits hacia la derecha rellenando con ceros, la diferencia con << es que este no conserva el bit de signo
			por lo que se toma siempre positivo
			-Existe el >> pero ese conserva el bit de signo, es decir, rellena con 1 cuando se desplaze a la derecha
			-(mask >>> 24) & 0xFF : desplaza los primeros 24 bits hacia la derecha dejando solo los 8 primeros bits,  solo quedaria 11111111=255
			el "& 0xFF" asegura que solo los 8 bits menos significativos sean tomados en cuenta
			-(mask >>> 16) : lo mismo que el anterior pero desplaza 16 bits hacia la derecha y nos quedamos con el segundo octecto es decir el menos significativo
			-(mask >>> 8) & 0xFF: lo mismo que los anteriores pero desplazamos 8 bits hacia la derecha y nos quedamos con el tercer octecto es decir el menos significativo
			-mask & 0xFF): aqui no se desplaza los bits sino simplemente seleccionamos los bits menos significativos que en este caso son el cuarto octeto.
 		*/
		return String.format("%d.%d.%d.%d",
				(mask >>> 24) & 0xFF,
				(mask >>> 16) & 0xFF,
				(mask >>> 8) & 0xFF,
				mask & 0xFF);
	}

	// Calcular la dirección de red aplicando una operación AND entre la IP y la máscara, el ipBytes[] es el arreglo de 4 bytes enviado en el metodo calcularRedInfo
	private byte[] calcularDireccionRed(byte[] ipBytes, int prefijo) {
		byte[] direccion_red = new byte[4];
		//realiza un desplazamiento a la izquierda por n posiciones
		int mask = 0xFFFFFFFF << (32 - prefijo);
		//El ciclo recorre las 4 posiciones de la direccion IP
		for (int i = 0; i < 4; i++) {
			/*
			ipBytes[i] es donde esta ubicado en la posicion de la direccion ip por ejemplo 192 o 168
			mask >>> (8 * (3 - i)) es el desplazamiento de las mascara hacia la derecha por n posiciones
			8 * (3 - i) garantiza que la mascara este alineada con el octeto de la direccion ip, en la primera iteracion i=0 por lo que se desplaza 24
			ipBytes[i] & (mask >>> (8 * (3 - i))) el icono & realiza una operación de AND bit a bit entre la porcion de ip y la porcion de mascara mascara
			Para prefijo 24
			En la primera iteración (i = 0), la máscara es 11111111 00000000 00000000 00000000.
			En la segunda iteración (i = 1), la máscara es 00000000 11111111 00000000 00000000 y se va haciendo la operacion AND con la IP
			 */
			direccion_red[i] = (byte) (ipBytes[i] & (mask >>> (8 * (3 - i))));
		}

		return direccion_red;
	}

	// Calcular la dirección de broadcast
	private byte[] calcularDireccionBroadcast(byte[] ipBytes, int prefijo) {

		byte[] direccion_broadcast = new byte[4];
		//realiza un desplazamiento a la izquierda por n posiciones
		int mask = 0xFFFFFFFF << (32 - prefijo);
		//El ciclo recorre las 4 posiciones de la direccion IP
		for (int i = 0; i < 4; i++) {
			/*
			aqui realicamos el mismo desplazamiento de la mascara con (mask >>> (8 * (3 - i))
			El ~ o operador NOT nos permite invertir los bits de la mascara los 0 en 1 y los 1 n 0
			& 0xFF se asegura de que el resultado sea de 8 bits.
			Se realizar | la operacion OR entre el octeto actual de la direccion ip ipBytes[i] y el resultado de inversion en la mascara
			Ejemplo para la direccion 192.168.1.10/24
			las mascara seria 255.255.255.0 en binario 11111111 11111111 11111111 00000000 con la inversion 00000000 00000000 00000000 11111111
			con la operacion OR
			Para el primer octeto: 11000000 | 00000000 = 11000000 (192).
			Para el segundo octeto: 10101000 | 00000000 = 10101000 (168).
			Para el tercer octeto: 00000001 | 00000000 = 00000001 (1).
			Para el cuarto octeto: 00001010 | 11111111 = 11111111 (255).
			Esto se hace ya que solo necesitamos realizar operaciones despues del prefijo de la mascara, es decir, los datos antes de la mascara lo necesitamos normal
			ya despues del prefijo se realizan operaciones
			 */
			direccion_broadcast[i] = (byte) (ipBytes[i] | (~(mask >>> (8 * (3 - i))) & 0xFF));
		}

		return direccion_broadcast;
	}

	// Convertir bytes a formato de dirección IP legible
	private String bytesToIp(byte[] bytes) {
		StringBuilder ip = new StringBuilder();
		//se recorre cada byte del del arreglo, que seran 4
		for (int i = 0; i < bytes.length; i++) {
			/*
			 -Nos trae el byte de la posicion i del arreglo
			 -& 0xFF convierte un byte con signo en un entero sin signo, manteniendo los bits originales es decir en vez de  -128 a 127 que son los bytes en Java con valores con signo
			 que vallan de 0 a 255, por ejemplo  -64 con & 0xFF obtenemos 192
			 */

			ip.append(bytes[i] & 0xFF);
			//Despues de cada octeto se agrega un punto sin incluir el ultimo
			if (i < bytes.length - 1) {
				ip.append(".");
			}
		}
		return ip.toString();
	}

	// Determinar la clase de la IP
	private String determinarClaseIP(byte[] ipBytes) {
		// acceder al primer octeto de la direccion,  & 0xFF convierte el byte en un numero sin signo
		int primerOcteto = ipBytes[0] & 0xFF;

		if (primerOcteto >= 0 && primerOcteto <= 127) {
			return "Clase A";
		} else if (primerOcteto >= 128 && primerOcteto <= 191) {
			return "Clase B";
		} else if (primerOcteto >= 192 && primerOcteto <= 223) {
			return "Clase C";
		} else {
			return "Clase Distinta";
		}
	}

	// Verificar si una IP es privada
	private boolean esIPPrivada(byte[] ipBytes) {
		int primerOcteto = ipBytes[0] & 0xFF;
		int segundoOcteto = ipBytes[1] & 0xFF;

		// Rango privado de Clase A: 10.0.0.0 a 10.255.255.255
		if (primerOcteto == 10) {
			return true;
		}

		// Rango privado de Clase B: 172.16.0.0 a 172.31.255.255
		if (primerOcteto == 172 && (segundoOcteto >= 16 && segundoOcteto <= 31)) {
			return true;
		}

		// Rango privado de Clase C: 192.168.0.0 a 192.168.255.255
		if (primerOcteto == 192 && segundoOcteto == 168) {
			return true;
		}
		// sino coincide ninguna, es publica
		return false;
	}

	// Método para generar la porción de red y de host en binario
	private void generarBinarios(byte[] ipBytes, int prefijo) {
		StringBuilder ipBinary = new StringBuilder();
		StringBuilder networkBinary = new StringBuilder();
		StringBuilder hostBinary = new StringBuilder();

		// Convertir la IP a binario
		for (byte b : ipBytes) {
			/*
			b & 0xFF= convierte el byte aun entero sin signo
			Integer.toBinaryString(b & 0xFF)= Convierte el valor de b a su representación binaria
			String.format("%8s", ...)= Se asegura que siempre se tenga 8 bits y se reemplaza los espacios en blancos con 0
			 */
			String binario = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
			/*
			Añade la representación binaria de cada byte a la cadena completa que representa la dirección IP en binario.
			Ejemplo: Si la dirección IP es 192.168.1.10, los bytes en binario se verían así:
			192 → 11000000
			168 → 10101000
			1 → 00000001
			10 → 00001010
			ipBinary=11000000000000010000000100001010
			 */
			ipBinary.append(binario);
		}

		/*
		 Recorre el ipBinary y
		 Divide en porciones de red y host según el prefijo
		 */
		for (int i = 0; i < ipBinary.length(); i++) {
			if (i < prefijo) {
				networkBinary.append(ipBinary.charAt(i));
			} else {
				hostBinary.append(ipBinary.charAt(i));
			}
		}

		// Guardar las porciones binarias
		this.parte_red_binaria = networkBinary.toString();
		this.parte_host_binaria = hostBinary.toString();
	}

	// Método para calcular el rango de IPs útiles
	private String calcularRangoIPs(byte[] networkBytes, byte[] broadcastBytes) {
		// Hacer copia de las direcciones de red y broadcast para que no afecten las originales
		byte[] primeraIPUtil = Arrays.copyOf(networkBytes, networkBytes.length);
		byte[] ultimaIPUtil = Arrays.copyOf(broadcastBytes, broadcastBytes.length);

		/*
		 La primera IP utilizable es la dirección de red + 1
		 primeraIPUtil[3] & 0xFF =convierte el valor del cuarto octeto de primeraIPUtil[3] a un valor sin signo
		 +1 se suma para dar con la siguiente dirección después de la dirección de red
		 */
		primeraIPUtil[3] = (byte) ((primeraIPUtil[3] & 0xFF) + 1);

		/*
		 La última IP utilizable es la dirección de broadcast - 1
		 -1, la dirección anterior a la dirección de broadcast.
		 */
		ultimaIPUtil[3] = (byte) ((ultimaIPUtil[3] & 0xFF) - 1);

		// Convertir las IPs útiles a formato legible
		String primeraIP = bytesToIp(primeraIPUtil);
		String ultimaIP = bytesToIp(ultimaIPUtil);

		return primeraIP + " - " + ultimaIP;
	}

	// Getters para acceder a los datos
	public String getIp() {
		return ip;
	}

	public String getMascara() {
		return mascara;
	}

	public String getDireccion_red() {
		return direccion_red;
	}

	public String getBroadcast() {
		return broadcast;
	}

	public int getNumero_host() {
		return numero_host;
	}

	public String getClaseIP() {
		return claseIP;
	}

	public String getTipoIP() {
		return tipoIP;
	}

	public String getParte_red_binaria() {
		return parte_red_binaria;
	}

	public String getParte_host_binaria() {
		return parte_host_binaria;
	}

	public String getRangoIPs() {
		return rangoIPs;
	}
}
