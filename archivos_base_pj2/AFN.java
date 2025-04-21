
import java.io.*;
import java.util.*;

public class AFN{

	private List<String> alfabeto;
	private int numEstados;
	private Set<Integer> estadosFinales;
	private List<List<Set<Integer>>> transiciones; // [simbolo][estado] -> conjunto de destinos
	private final int estadoInicial = 1;

	public AFN(String path){
		try {
			File AFDFile = new File(path);
			Scanner myReader = new Scanner(AFDFile);

			// 1. Leer alfabeto
			String[] simbolos = myReader.nextLine().split(",");
			alfabeto = Arrays.asList(simbolos);
			System.out.println("Alfabeto del AFN: " + alfabeto);

			// 2. Leer número de estados
			numEstados = Integer.parseInt(myReader.nextLine().trim());
			System.out.println("Número de estados: " + numEstados);

			// 3. Leer estados finales
			estadosFinales = new HashSet<>();
			String[] finales = myReader.nextLine().split(",");
			for (String f : finales) {
				if (!f.trim().isEmpty()) {
					estadosFinales.add(Integer.parseInt(f.trim()));
				}
			}
			System.out.println("Estados finales: " + estadosFinales);

			// 4. Inicializar matriz de transiciones
			int filas = alfabeto.size() + 1; // +1 por la fila de lambda
			transiciones = new ArrayList<>();

			int i = 0;
			while (i < filas && myReader.hasNextLine()) {
				String[] linea = myReader.nextLine().split(",");
				List<Set<Integer>> filaTransicion = new ArrayList<>();

				for (String celda : linea) {
					Set<Integer> destinos = new HashSet<>();
					if (!celda.trim().equals("0")) {
						String[] destinosSplit = celda.split(";");
						for (String d : destinosSplit) {
							destinos.add(Integer.parseInt(d.trim()));
						}
					}
					filaTransicion.add(destinos);
				}
				transiciones.add(filaTransicion);
				i++;
			}

			myReader.close();

		} catch (FileNotFoundException e) {
			System.out.println("Archivo no encontrado: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean accept(String palabra) {
		Set<Integer> S = clausuraLambda(Set.of(estadoInicial)); //S=clausura(f(S,c))
		int index = 0;

		//mientras c no sea el fin de la palabra
		while (index < palabra.length()) {
			char c = palabra.charAt(index); //c es el primer caracter de la palabra
			String simbolo = String.valueOf(c);

			//Realizar la transicion
			Set<Integer> move = new HashSet<>();
			for (int estado : S) {
				int simboloIndex = alfabeto.indexOf(simbolo);
				if (simboloIndex != -1) {
					move.addAll(transiciones.get(simboloIndex + 1).get(estado)); //obtener los estados resultantes de la transicion
				}
			}

			//actualizar s con la clausura de los nuevos estados
			S = clausuraLambda(move);
			index++;
		}

		//verificar si hay interseccion entre S y estados finales
		for (int estado : S) {
			if (estadosFinales.contains(estado)) {
				return true; //la palabra fue reconocida
			}
		}
		return false; //la palabra no fue reconocida
	}

	private Set<Integer> clausuraLambda(Set<Integer> estados) {
		Set<Integer> clausura = new HashSet<>(estados); //clausura = {q}
		Set<Integer> auxiliar = new HashSet<>(); // auxiliar es vacio

		while (!auxiliar.equals(clausura)) {
			auxiliar = new HashSet<>(clausura);
			for (int p : auxiliar) {
				Set<Integer> destinos = transiciones.get(0).get(p);
				for (int s : destinos) {
					clausura.add(s);
				}
			}
		}
		return clausura;
	}

	public void toAFD(String afdPath){
	Map<Set<Integer>, Integer> stateIds = new HashMap<>();
	Map<Integer, Map<String, Integer>> afdTransitions = new HashMap<>();
	Set<Integer> afdFinalStates = new HashSet<>();
	Queue<Set<Integer>> queue = new LinkedList<>();

	// Paso 1: calcular la clausura lambda del estado inicial
	Set<Integer> estadoInicialClausura = clausuraLambda(Set.of(estadoInicial));
	stateIds.put(estadoInicialClausura, 0);
	queue.add(estadoInicialClausura);
	int siguienteId = 1;

	// Paso 2: explorar todos los subconjuntos
	while (!queue.isEmpty()) {
		Set<Integer> actual = queue.poll();
		int actualId = stateIds.get(actual);
		afdTransitions.putIfAbsent(actualId, new HashMap<>()); //si ya existe una entrada con la misma clave y su valor no es nulo, el hashmap no se modifica.

		for (int i = 0; i < alfabeto.size(); i++) {
			String simbolo = alfabeto.get(i);
			Set<Integer> destinos = new HashSet<>();

			// Para cada estado del subconjunto actual
			for (int estado : actual) {
				// Obtener destinos directos por el símbolo
				destinos.addAll(transiciones.get(i + 1).get(estado));
			}

			// Aplicar clausura lambda al conjunto de destinos
			Set<Integer> clausura = clausuraLambda(destinos);

			if (!stateIds.containsKey(clausura)) {
				stateIds.put(clausura, siguienteId++);
				queue.add(clausura);
			}
			afdTransitions.get(actualId).put(simbolo, stateIds.get(clausura));
		}
	}

	// Paso 3: identificar los estados finales del AFD
	for (Map.Entry<Set<Integer>, Integer> entry : stateIds.entrySet()) {
		for (int estado : entry.getKey()) {
			if (estadosFinales.contains(estado)) {
				afdFinalStates.add(entry.getValue());
				break;
			}
		}
	}

	// escribir en archivo
	try (PrintWriter writer = new PrintWriter(afdPath)) {
		writer.println(String.join(",", alfabeto));
		writer.println(stateIds.size());
		writer.println(String.join(",", afdFinalStates.stream().map(String::valueOf).toArray(String[]::new)));

		// para cada símbolo, escribir la fila correspondiente
		for (int i = 0; i < alfabeto.size(); i++) {
			String simbolo = alfabeto.get(i);
			for (int s = 0; s < stateIds.size(); s++) {
				Integer destino = afdTransitions.getOrDefault(s, new HashMap<>()).get(simbolo);
				writer.print((destino != null ? destino : 0));
				if (s < stateIds.size() - 1) writer.print(",");
			}
			writer.println();
		}

		System.out.println("AFD guardado en: " + afdPath);

	} catch (IOException e) {
		System.out.println("Error al escribir el AFD: " + e.getMessage());
	}
}


	public static void main(String[] args) throws Exception{
		AFN afn = new AFN(args[0]);
		if (args.length > 2 && args[1].equals("-to-afd")) {
			afn.toAFD(args[2]);
		} else {
			Scanner sc = new Scanner(System.in);
			String linea;
			while (!(linea = sc.nextLine()).equals("")) {
				System.out.println(afn.accept(linea));
			}
			sc.close();
		}
	}
}
