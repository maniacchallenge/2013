package de.fu_berlin.maniac.strategies.auxclasses;

import java.util.ArrayList;

public class Dijkstra {
	
	public Dijkstra() {
		//Pegar tudo do OlsrDataDump
		//Pegar a Collection de Links
		//Converter todos os IPs para int
		//Montar o grafo a partir de localIP, remoteIP e linkcost (arestas)
	}

	// Dijkstra's algorithm to find shortest path from s to all other nodes
	public static int[] dijkstra(WeightedGraph G, int s) {
		final int[] dist = new int[G.size()]; // shortest known distance
												// from "s"
		final int[] pred = new int[G.size()]; // preceeding node in path
		final boolean[] visited = new boolean[G.size()]; // all false
															// initially

		for (int i = 0; i < dist.length; i++) {
			dist[i] = Integer.MAX_VALUE;
		}
		dist[s] = 0;

		for (int i = 0; i < dist.length; i++) {
			final int next = minVertex(dist, visited);
			visited[next] = true;

			// The shortest path to next is dist[next] and via pred[next].

			final int[] n = G.neighbors(next);
			for (int j = 0; j < n.length; j++) {
				final int v = n[j];
				final int d = dist[next] + G.getWeight(next, v);
				if (dist[v] > d) {
					dist[v] = d;
					pred[v] = next;
				}
			}
		}
		return pred; // (ignore pred[s]==0!)
	}

	private static int minVertex(int[] dist, boolean[] v) {
		int x = Integer.MAX_VALUE;
		//FIXME HAVERÁ "ILHAS"?? ESSSA É A MELHOR SOLUÇÃO? LEMBRAR QUE ANTES y = -1 E OCORRIA ERRO;
		int y = dist.length - 1; // graph not connected, or no unvisited vertices
		for (int i = 0; i < dist.length; i++) {
			if (!v[i] && dist[i] < x) {
				y = i;
				x = dist[i];
			}
		}
		return y;
	}

	public static void printPath(WeightedGraph G, int[] pred, int s, int e) {
		final ArrayList path = new ArrayList();
		int x = e;
		while (x != s) {
			path.add(0, G.getLabel(x));
			x = pred[x];
		}
		path.add(0, G.getLabel(s));
		//System.out.println(path);
	}
	
	//CHECAR
	public static int menorCaminho(WeightedGraph G, int[] pred, int s, int e) {
		final ArrayList path = new ArrayList();
		int x = e;
		while (x != s) {
			path.add(0, G.getLabel(x));
			x = pred[x];
		}
		path.add(0, G.getLabel(s));
		int dist = path.size() - 1;
		return dist;
	}

}
