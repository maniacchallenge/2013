//This code represents our the strategy for the Maniac Challenge 2013
//The other classes contains some aux methods that help the work of this strategy 

/*
 * @author UnB TEAM - Tightness strategy
 * @date August 2013
 */


package de.fu_berlin.maniac.strategies;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import de.fu_berlin.maniac.exception.ManiacException;
import de.fu_berlin.maniac.general.AuctionParameters;
import de.fu_berlin.maniac.general.ManiacStrategyInterface;
import de.fu_berlin.maniac.general.Mothership;
import de.fu_berlin.maniac.network_manager.NodePair;
import de.fu_berlin.maniac.network_manager.TopologyInfo;
import de.fu_berlin.maniac.packet_builder.*;
import de.fu_berlin.maniac.strategies.auxclasses.Dijkstra;
import de.fu_berlin.maniac.strategies.auxclasses.ImportantMethods;
import de.fu_berlin.maniac.strategies.auxclasses.Pair;
import de.fu_berlin.maniac.strategies.auxclasses.WeightedGraph;

public class NerdsStrategy implements ManiacStrategyInterface {

	//Quem ganha minha auction
	Bid winner;

	//RELACIONAMENTOS COM TRANS_ID
	ArrayList<Pair<Integer, Integer>> Bn_list = new ArrayList<Pair<Integer, Integer>>();
	ArrayList<Pair<Integer, Data>> my_data_packet_list = new ArrayList<Pair<Integer, Data>>();
	ArrayList<Pair<Integer, Integer>> Fn_list = new ArrayList<Pair<Integer, Integer>>();
	ArrayList<Pair<Integer, BidWin>> winner_list = new ArrayList<Pair<Integer, BidWin>>();
	ArrayList<Pair<Integer, Integer>> my_bid_list = new ArrayList<Pair<Integer, Integer>>();
	ArrayList<Pair<Integer, Integer>> initial_budget_list = new ArrayList<Pair<Integer, Integer>>();	
	ArrayList<Pair<Integer, Inet4Address>> final_destination_list = new ArrayList<Pair<Integer, Inet4Address>>();
	
	ArrayList<Pair<Integer, Inet4Address>> whopassedpacket_list = new ArrayList<Pair<Integer, Inet4Address>>();
	

	//O grafo com a topologia. Será recalculado toda vez que formos decidir o nosso bid
	WeightedGraph t;
	
	ImportantMethods im;
	
	//TODO variar este valor para mudar a declividade
	Double cte = 1.0;
	Integer time_to_sleep;
	Integer execution_time = 1000;
	
	//Cap III
	Double budget_part = 0.6;
	Double fine_part = 0.9;
	
	//Cap IV
	Double k1 = 2.0;
	Double k2 = 3.0;
	
	InetAddress second_tablet;
	
	InetAddress deviceIP;
	
	public NerdsStrategy() {
		//TODO NO EVENTO - SETAR AQUI CORRETAMENTE O IP DO SEGUNDO TABLET
		try {
			deviceIP = (InetAddress) InetAddress.getByName("172.16.17.229");
			second_tablet = (InetAddress) InetAddress.getByName("172.16.17.222");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}	
	
	//A princípio nossa estratégia de bid não leva em consideração as dos "concorrentes"
	//Talvez seja bom levar esses valores em consideração
	//É bom aumentá-lo para não sermos sempre os primeiros
	//Retornando 0, pois nossa estratégia não espera antes de dar o bid para avaliar os outros bids
	//Entretanto temos de pensar em algum valor entre 0 e AUCTION_TIMEOUT para não sermos as "ovelhas"
	@Override
	public Long onRcvAdvert(Advert adv) {
		//Entre 0.5AT e 0.75AT
		int AT = Mothership.getAuctionTimeout();
		double i = ((Math.random() * 1000) % 250 + 500);
		
		long wait_time = (long) i * AT / 1000;
		
		return 0L;
		//return wait_time;
	}
	
	//Capítulo II. The bidding strategy
	//Parâmetros:
	//Timeout em número de saltos - H0_pu
	//Verificar a quantidade de saltos até o destino pelo menor caminho - hcn
	//Saber o menor caminho de todos os vizinhos que participam da auction até o destino - hcmin 
	//Calcula a função aperto em relação a média - cn
	//Calcula a função aperto em relação ao melhor vizinho (aquele menor caminho até o destino) - an
	//Calcular nossa oferta e retornar este bid (arrendondar baseado em nosso "desejo de ganhar") - O
	@Override
	public Integer sendBid(Advert adv) {
		
		int Bu = adv.getCeil();
		int Fu = adv.getFine();
		Integer Ho_pu = adv.getDeadline();
		
		Integer hcn = 0, hcmin = Integer.MAX_VALUE;
		double hcbarra = 0;
		ArrayList<Integer> hc = new ArrayList<Integer>();
		ArrayList<Integer> deltai = new ArrayList<Integer>();
		ArrayList<NodePair> top = null;
		try {
			top = TopologyInfo.getTopology();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} 
		
		//TODO NO EVENTO - trocar aqui pela quantidade exata!
		Integer ct_helding = 45; //12 tablets dos concorrentes + 23 backbones		
		t = new WeightedGraph(ct_helding);
		
		im = new ImportantMethods();
		
		for (NodePair pair : top) {
			int src = im.fromInetToInt(pair.getLastHopIP());
			int dst = im.fromInetToInt(pair.getDestinationIP());
			int cst = 1;

			t.setLabel(src, "v"+src);
			t.setLabel(dst, "v"+dst);
			
			t.addEdge(src, dst, cst);
		}
		

		Integer destino = im.fromInetToInt(adv.getFinalDestinationIP());
		Integer n = im.fromInetToInt(deviceIP);
		//Calcula minimum-spanning tree para o nó n
		int[] pred;
		pred = Dijkstra.dijkstra(t, n);

		//Rodar o djikstra para encontrar melhor caminho entre eu e o destino (adv.getFinalDestination())
		hcn = Dijkstra.menorCaminho(t, pred, n, destino);
		 
		ArrayList<InetAddress> nodes = new ArrayList<InetAddress>();
		int[] vizinhos = t.neighbors(im.fromInetToInt(adv.getSourceIP()));
		for (int i = 0; i < vizinhos.length; i++) {
			nodes.add(im.fromIntToInet(vizinhos[i]));
		}

		for (InetAddress node : nodes) {			
			//Se o cara já participou desta transmissão, não faz conta ele
			if (Pair.hasThatId(whopassedpacket_list, adv.getTransactionID(), (Inet4Address) node)) {
				continue;
			}
			
			Integer c = im.fromInetToInt(node);
			pred = Dijkstra.dijkstra(t, c);
			Integer min_dist = Dijkstra.menorCaminho(t, pred, c, destino);
			if (((Ho_pu - 1) - min_dist) >= 0) {
				hc.add(min_dist);
				deltai.add((Ho_pu - 1) - min_dist);
			}
		}
		
		hcbarra = ImportantMethods.calculateAverage(hc);  
		hcmin = ImportantMethods.calculateMinValue(hc);
		
		int deltan = (Ho_pu - 1) - hcn;

		int index;
		
		if (Bu == Fu && deltan >= 0 && Bu != 0) {
			int bidnnn = (int) Math.floor(0.9*Bu) - 1;
			index = Pair.idExists(my_bid_list, adv.getTransactionID());
			if (index >= my_bid_list.size()) {
				my_bid_list.add(null);
			}
			my_bid_list.set(index, Pair.createPair(adv.getTransactionID(), bidnnn));
			
			//ActionTimeout = 3000;
			time_to_sleep = Mothership.getAuctionTimeout() - execution_time;
			//Pause for 2 seconds
			try {
				Thread.sleep(time_to_sleep);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			return bidnnn;
		}
		
		double delta_barra = ((Ho_pu - 1) - hcbarra);
		
		if (deltan == 0 && delta_barra == 0) {
			deltan = 1;
			delta_barra = 1;
			
			hcmin = Ho_pu - 2;
		}
		
		//Se deltan é menor ou igual a zero, retornamos Bu
		//O valor retornado poderia ser alterado dependendo da situação dos nossos concorrentes (que podem estar todos apertados igualmente, por exemplo)
		if (deltan <= 0) {
			int bidnnn = Bu;
			index = Pair.idExists(my_bid_list, adv.getTransactionID());
			if (index >= my_bid_list.size()) {
				my_bid_list.add(null);
			}
			my_bid_list.set(index, Pair.createPair(adv.getTransactionID(), bidnnn));
			
			//ActionTimeout = 3000;
			time_to_sleep = Mothership.getAuctionTimeout() - execution_time;
			//Pause for 2 seconds
			try {
				Thread.sleep(time_to_sleep);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			return bidnnn;
		}		
		//Se deltan maior que zero e nenhum outro nó é capaz de entregar a tempo, retornamos Bu
		//"if there is no competition, i.e., we are the only node reachable by the upstream node, we set our bid to the maximum value Bu"
		if (hc.size() == 0) {
			int bidnnn = Bu;
			index = Pair.idExists(my_bid_list, adv.getTransactionID());
			if (index >= my_bid_list.size()) {
				my_bid_list.add(null);
			}
			my_bid_list.set(index, Pair.createPair(adv.getTransactionID(), bidnnn));
			
			//ActionTimeout = 3000;
			time_to_sleep = Mothership.getAuctionTimeout() - execution_time;
			//Pause for 2 seconds
			try {
				Thread.sleep(time_to_sleep);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			return bidnnn;
		}
		
		Double cn = deltan/delta_barra;
		int delta_max = (Ho_pu - 1) - hcmin;
		Integer an = deltan / delta_max;
		
		//Arredondando sempre pra baixo. Manter ou aumentar pra cima quando não queremos ganhar? Ou também quando a relação entre Bu e Fu for desvantajosa 		
		Integer O = (int) Math.floor((Bu - Fu)*(1 - (1 / (1 + Math.exp(-an * cte * (cn - 1))))) + Fu);
		
		index = Pair.idExists(initial_budget_list, adv.getTransactionID());
		if (index >= initial_budget_list.size()) {
			initial_budget_list.add(null);
		}
		initial_budget_list.set(index, Pair.createPair(adv.getTransactionID(), adv.getInitialBudget()));			
		
		index = Pair.idExists(my_bid_list, adv.getTransactionID());
		if (index >= my_bid_list.size()) {
			my_bid_list.add(null);
		}
		my_bid_list.set(index, Pair.createPair(adv.getTransactionID(), O));
		
		//ActionTimeout = 3000;
		time_to_sleep = Mothership.getAuctionTimeout() - execution_time;
		//Pause for 2 seconds
		try {
			Thread.sleep(time_to_sleep);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return O;
		
	}

	//Guardar em arquivos de log - na classe ManiacLogger
	//Pensar em usar estes dados na nossa estratégia
	@Override
	public void onRcvBid(Bid bid) {
		//TODO guardar um log
	}

	//Guardar em arquivos de log - na classe ManiacLogger
	//Pensar em usar estes dados na nossa estratégia
	@Override
	public void onRcvBidWin(BidWin bidwin) {
		whopassedpacket_list.add(Pair.createPair(bidwin.getTransactionID(), bidwin.getWinnerIP()));
		
		//Seta a variável que será usada em onRcvData() para determinar o valor da nossa auction
		
		int index;
		index = Pair.idExists(winner_list, bidwin.getTransactionID());
		if (index >= winner_list.size()) {
			winner_list.add(null);
		}
		winner_list.set(index, Pair.createPair(bidwin.getTransactionID(), bidwin));
	}

	//Capítulo III
	@Override
	public AuctionParameters onRcvData(Data packet) {
		//Guardar o destino dessa Auction
		int index;
		index = Pair.idExists(final_destination_list, packet.getTransactionID());
		if (index >= final_destination_list.size()) {
			final_destination_list.add(null);
		}
		final_destination_list.set(index, Pair.createPair(packet.getTransactionID(), packet.getFinalDestinationIP()));
		
		index = Pair.idExists(my_data_packet_list, packet.getTransactionID());
		if (index >= my_data_packet_list.size()) {
			my_data_packet_list.add(null);
		}
		my_data_packet_list.set(index, Pair.createPair(packet.getTransactionID(), packet));

		//Instanciar um objeto do AuctionParameters
		//Setar o atributos maxbid (60% do vencedor) - maxbid
		//Setar o atributo fine (90% do maxbid) - fine
		int Bn, Fn;
		
		BidWin last_winner = Pair.getMap(winner_list, packet.getTransactionID());
		
		Bn = (int) Math.floor(last_winner.getWinningBid() * budget_part);
		Fn = (int) Math.ceil(Bn * fine_part);

		index = Pair.idExists(Bn_list, packet.getTransactionID());
		if (index >= Bn_list.size()) {
			Bn_list.add(null);
		}
		Bn_list.set(index, Pair.createPair(packet.getTransactionID(), Bn));
		
		index = Pair.idExists(Fn_list, packet.getTransactionID());
		if (index >= Fn_list.size()) {
			Fn_list.add(null);
		}
		Fn_list.set(index, Pair.createPair(packet.getTransactionID(), Fn));
		
		AuctionParameters nerds = new AuctionParameters(Bn, Fn);
		return nerds;

	}

	//Capítulo IV - Request for bids set up strategy
	//Pegar os bids e as funções aperto de todos e calcular a função de preferência
	//k2 > k1
	//O vencedor é o com maior função de preferência
	@Override
	public Bid selectWinner(List<Bid> bids) {

		winner = null;
		
		Bid bidexemple = null;		
		for (Bid bid : bids) {
			bidexemple = bid;
			break;
		}
		
		Inet4Address last_final_destination = Pair.getMap(final_destination_list, bidexemple.getTransactionID());		
		Data my_last_data_packet = Pair.getMap(my_data_packet_list, bidexemple.getTransactionID());
		Integer last_Bn = Pair.getMap(Bn_list, bidexemple.getTransactionID());		
		
		Integer my_last_bid = Pair.getMap(my_bid_list, bidexemple.getTransactionID());		
		Integer budgetoriginal = Pair.getMap(initial_budget_list, bidexemple.getTransactionID());
		
		ArrayList<Integer> hc = new ArrayList<Integer>();
		ArrayList<Integer> deltai = new ArrayList<Integer>();
		int[] pred;
		Integer destino = im.fromInetToInt(last_final_destination);

		Integer Ho_pu = my_last_data_packet.getHopCount();
		for (Bid bid : bids) {				
			Integer c = im.fromInetToInt(bid.getSourceIP());
			pred = Dijkstra.dijkstra(t, c);
			int min_dist = Dijkstra.menorCaminho(t, pred, c, destino);
			deltai.add((Ho_pu - 1) - min_dist);
			if (((Ho_pu - 1) - min_dist) >= 0) {
				hc.add(min_dist);
			}
		}
		
		//Verificar quando vale a pena mandar o pacote para o backbone
		//Comparar a multa do upstream com o budget do backbone
		if (Ho_pu == 1 && budgetoriginal - my_last_bid <= my_last_data_packet.getFine()) {
			//Manda para o backbone
			return winner;
		}
		//Caso não caia no if, paga a multa e repassa o pacote

		double cmax = Integer.MIN_VALUE, Pn;
		
		double[] ci = new double[bids.size()];
		double hcbarra = ImportantMethods.calculateAverage(hc); 
		int i = 0;
		for (Bid bid : bids) { 								
			if (deltai.get(i) < 0) {
				ci[i] = -1000;
			} else {
				int deltan = deltai.get(i);
				
				//SE É O NOSSO SEGUNDO TABLET E DELTAN MAIOR QUE ZERO 
				if (bid.getSourceIP().equals(second_tablet) && deltan > 0) {
					return bid;
				}
				
				double delta_barra = (Ho_pu - 1) - hcbarra;
				if (deltan == 0 && delta_barra == 0) {
					deltan = 1;
					delta_barra = 1.0;
				}
				ci[i] = deltan/delta_barra;
				if (ci[i] > cmax) {
					cmax = ci[i];
				}
				
				
			}
			
			i++;
		}
		
		boolean pelomenosum = true;
		//SE NENHUM BID FOR ACESSÍVEL AO DESTINO!!
		if (hc.size() == 0) {
			cmax = 1;
			pelomenosum = false;
		}
		ArrayList<Double> P = new ArrayList<Double>();
		double max = Integer.MIN_VALUE;
		i = 0;				
		for (Bid bid : bids) {
			//SE NÃO TIVER NENHUM BID ACESSÍVEL AO DESTINO, ENVIAR PARA QUALQUER UM DIFERENTE DO MEU SEGUNDO TABLET
			if (!pelomenosum && !bid.getSourceIP().equals(second_tablet)) {
				return bid;
			}
			Pn = k1 - (k1 / last_Bn) * bid.getBid() + (k2 / cmax) * ci[i];
			P.add(Pn);
			//Se falta somente 1 hop (perda total), não pode ser meu tablet. Se falta mais de 1, tanto faz.
			if (Pn > max && (Ho_pu == 1 && !bid.getSourceIP().equals(second_tablet) || Ho_pu != 1)) {
				max = Pn;
				winner = bid;
			}
			i++;
		}
		
		return winner;
	}

	//Queremos que alguns pacotes não sejam entregues antes do leilão?
	@Override
	public boolean dropPacketBefore(Data buffer_data) {
		//NetworkManager nm = NetworkManager.getInstance();
		//InetAddress backbone = nm.getMyOwnBackbone();
		
		Integer my_last_bid = Pair.getMap(my_bid_list, buffer_data.getTransactionID());		
		
		if (my_last_bid == 0 && buffer_data.getFine() == 0) {
			return true;
		}
		return false;
	}

	//Queremos que alguns pacotes não sejam entregues depois do leilão?
	@Override
	public boolean dropPacketAfter(Data buffer_data) {
		//NetworkManager nm = NetworkManager.getInstance();
		//InetAddress backbone = nm.getMyOwnBackbone();
		return false;
	}

	@Override
	public void onException(ManiacException ex, boolean fatal) {

	}

}