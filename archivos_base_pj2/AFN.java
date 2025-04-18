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
		Set<Integer> S = clausuraLambda(Set.of(estadoInicial));
		int index = 0;

		while (index < palabra.length()) {
			char c = palabra.charAt(index);
			String simbolo = String.valueOf(c);

			Set<Integer> move = new HashSet<>();
			for (int estado : S) {
				int simboloIndex = alfabeto.indexOf(simbolo);
				if (simboloIndex != -1) {
					move.addAll(transiciones.get(simboloIndex + 1).get(estado));
				}
			}

			S = clausuraLambda(move);
			index++;
		}

		for (int estado : S) {
			if (estadosFinales.contains(estado)) {
				return true;
			}
		}
		return false;
	}

	private Set<Integer> clausuraLambda(Set<Integer> estados) {
		Set<Integer> clausura = new HashSet<>(estados);
		Set<Integer> auxiliar = new HashSet<>();

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
	}

	public static void main(String[] args) throws Exception{
		String file = args[0];
		new AFN(file);
	}
}
