package net.commotionwireless.olsrinfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.commotionwireless.olsrinfo.datatypes.Config;
import net.commotionwireless.olsrinfo.datatypes.Gateway;
import net.commotionwireless.olsrinfo.datatypes.HNA;
import net.commotionwireless.olsrinfo.datatypes.Interface;
import net.commotionwireless.olsrinfo.datatypes.Link;
import net.commotionwireless.olsrinfo.datatypes.MID;
import net.commotionwireless.olsrinfo.datatypes.Neighbor;
import net.commotionwireless.olsrinfo.datatypes.Node;
import net.commotionwireless.olsrinfo.datatypes.OlsrDataDump;
import net.commotionwireless.olsrinfo.datatypes.Plugin;
import net.commotionwireless.olsrinfo.datatypes.Route;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Parse the output of <tt>olsrd</tt>'s jsoninfo plugin, using the
 * Jackson JSON library to parse the data into Java objects. 
 * 
 * Written as part of the Commotion Wireless project
 * 
 * @author Hans-Christoph Steiner <hans@eds.org>
 * @see <a href="https://code.commotionwireless.net/projects/commotion/wiki/OLSR_Configuration_and_Management">OLSR Configuration and Management</a>
 */
public class JsonInfo {

	private String lastCommand = "";

	String host = "127.0.0.1";
	int port = 9090;

	ObjectMapper mapper = null;

	final Set<String> supportedCommands = new HashSet<String>(
			Arrays.asList(new String[] {
					// combined reports
					"all", // all of the JSON info
					"runtime", // all of the runtime status reports
					"startup", // all of the startup config reports
					// individual runtime reports
					"gateways", // gateways
					"hna", // Host and Network Association
					"interfaces", // network interfaces
					"links", // links
					"mid", // MID
					"neighbors", // neighbors
					"routes", // routes
					"topology", // mesh network topology
					"runtime", // all the runtime info in a single report
					// the rest don't change at runtime, so they're separate
					"config", // the current running config info
					"plugins", // loaded plugins and their config
					// only non-JSON output, can't be combined with others
					"olsrd.conf", // current config in olsrd.conf format
			}));

	public JsonInfo() {
	}

	public JsonInfo(String sethost) {
		host = sethost;
	}

	public JsonInfo(String sethost, int setport) {
		host = sethost;
		port = setport;
	}

	private boolean isCommandStringValid(String cmdString) {
		boolean isValid = true;
		if (!cmdString.equals(lastCommand)) {
			lastCommand = cmdString;
			for (String s : cmdString.split("/")) {
				if ( !s.equals("") && !supportedCommands.contains(s)) {
					System.out.println("Unsupported command: " + s);
					isValid = false;
				}
			}
		}
		return isValid;
	}
	
	/**
	 * Request a reply from the jsoninfo plugin via a network socket.
	 * 
	 * @param The command to query jsoninfo with
	 * @return A String array of the result, line-by-line
	 * @throws IOException when it cannot get a result.
	 */
	String[] request(String req) throws IOException {
		Socket sock = null;
		BufferedReader in = null;
		PrintWriter out = null;
		List<String> retlist = new ArrayList<String>();

		try {
			sock = new Socket(host, port);
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()), 8192);
			out = new PrintWriter(sock.getOutputStream(), true);
		} catch (UnknownHostException e) {
			System.err.println("Unknown host: " + host);
			return new String[0];
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for socket to " + host + ":"
					+ Integer.toString(port));
			return new String[0];
		}
		out.println(req);
		String line;
		while ((line = in.readLine()) != null) {
			if (!line.equals(""))
				retlist.add(line);
		}
		// the jsoninfo plugin drops the connection once it outputs
		out.close();
		in.close();
		sock.close();

		return retlist.toArray(new String[retlist.size()]);
	}

	/**
	 * Send a command to the jsoninfo plugin and get the raw dump back.
	 * 
	 * @param The command to query jsoninfo with
	 * @return The complete JSON from jsoninfo as single String
	 */
	public String command(String cmdString) {
		String[] data = new String[0];
		String ret = "";

		isCommandStringValid(cmdString);
		try {
			data = request(cmdString);
		} catch (IOException e) {
			System.err.println("Failed to read data from " + host + ":"
					+ Integer.toString(port));
		}
		for (String s : data) {
			ret += s + "\n";
		}
		return ret;
	}

	/**
	 * Query the jsoninfo plugin over a network socket and return the results 
	 * parsed into Java objects.
	 * 
	 * @param The command to query jsoninfo with
	 * @return The complete JSON reply parsed into Java objects.
	 */
	public OlsrDataDump parseCommand(String cmd) {
		if (mapper == null)
			mapper = new ObjectMapper();
		OlsrDataDump ret = new OlsrDataDump();
		try {
			String dump = command(cmd);
			if (! dump.contentEquals(""))
				ret = mapper.readValue(dump, OlsrDataDump.class);
			ret.setRaw(dump);
			/*
			// TODO Jackson is happier if it can download the stuff itself
			// http://wiki.fasterxml.com/JacksonBestPracticesPerformance
			URL url = new URL("http", host, port, cmd);
			ret = mapper.readValue(url, OlsrDataDump.class);
			*/
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// change nulls to blank objects so you can use this result in a for()
		if (ret.config == null)
			ret.config = new Config();
		if (ret.gateways == null)
			ret.gateways = Collections.emptyList();
		if (ret.hna == null)
			ret.hna = Collections.emptyList();
		if (ret.interfaces == null)
			ret.interfaces = Collections.emptyList();
		if (ret.links == null)
			ret.links = Collections.emptyList();
		if (ret.mid == null)
			ret.mid = Collections.emptyList();
		if (ret.neighbors == null)
			ret.neighbors = Collections.emptyList();
		if (ret.topology == null)
			ret.topology = Collections.emptyList();
		if (ret.plugins == null)
			ret.plugins = Collections.emptyList();
		if (ret.routes == null)
			ret.routes = Collections.emptyList();
		return ret;
	}

	/**
	 * all of the runtime and startup status information in a single report
	 * 
	 * @return array of per-IP arrays of IP address, SYM, MPR, MPRS,
	 *         Willingness, and 2 Hop Neighbors
	 */
	public OlsrDataDump all() {
		return parseCommand("/all");
	}

	/**
	 * all of the runtime status information in a single report
	 * 
	 * @return array of per-IP arrays of IP address, SYM, MPR, MPRS,
	 *         Willingness, and 2 Hop Neighbors
	 */
	public OlsrDataDump runtime() {
		return parseCommand("/interfaces");
	}

	/**
	 * all of the startup config information in a single report
	 * 
	 * @return array of per-IP arrays of IP address, SYM, MPR, MPRS,
	 *         Willingness, and 2 Hop Neighbors
	 */
	public OlsrDataDump startup() {
		return parseCommand("/interfaces");
	}

	/**
	 * immediate neighbors on the mesh
	 * 
	 * @return array of per-IP arrays of IP address, SYM, MPR, MPRS,
	 *         Willingness, and 2 Hop Neighbors
	 */
	public Collection<Neighbor> neighbors() {
		return parseCommand("/neighbors").neighbors;
	}

	/**
	 * direct connections on the mesh, i.e. nodes with direct IP connectivity
	 * via Ad-hoc
	 * 
	 * @return array of per-IP arrays of Local IP, Remote IP, Hysteresis, LQ,
	 *         NLQ, and Cost
	 */
	public Collection<Link> links() {
		return parseCommand("/links").links;
	}

	/**
	 * IP routes to nodes on the mesh
	 * 
	 * @return array of per-IP arrays of Destination, Gateway IP, Metric, ETX,
	 *         and Interface
	 */
	public Collection<Route> routes() {
		return parseCommand("/routes").routes;
	}

	/**
	 * Host and Network Association (for supporting dynamic internet gateways)
	 * 
	 * @return array of per-IP arrays of Destination and Gateway
	 */
	public Collection<HNA> hna() {
		return parseCommand("/hna").hna;
	}

	/**
	 * Multiple Interface Declaration
	 * 
	 * @return array of per-IP arrays of IP address and Aliases
	 */
	public Collection<MID> mid() {
		return parseCommand("/mid").mid;
	}

	/**
	 * topology of the whole mesh
	 * 
	 * @return array of per-IP arrays of Destination IP, Last hop IP, LQ, NLQ,
	 *         and Cost
	 */
	public Collection<Node> topology() {
		return parseCommand("/topology").topology;
	}

	/**
	 * the network interfaces that olsrd is aware of
	 * 
	 * @return array of per-IP arrays of Destination IP, Last hop IP, LQ, NLQ,
	 *         and Cost
	 */
	public Collection<Interface> interfaces() {
		return parseCommand("/interfaces").interfaces;
	}

	/**
	 * the gateways to other networks that this node knows about
	 * 
	 * @return array of per-IP arrays of Status, Gateway IP, ETX, Hopcount,
	 *         Uplink, Downlink, IPv4, IPv6, Prefix
	 */
	public Collection<Gateway> gateways() {
		return parseCommand("/gateways").gateways;
	}

	/**
	 * The parsed configuration of olsrd in its current state
	 */
	public Config config() {
		return parseCommand("/config").config;
	}

	/**
	 * The parsed configuration of plugins in their current state
	 */
	public Collection<Plugin> plugins() {
		return parseCommand("/plugins").plugins;
	}

	/**
	 * The current olsrd configuration in the olsrd.conf format, NOT json
	 */
	public String olsrdconf() {
		return command("/olsrd.conf");
	}

	/**
	 * for testing from the command line
	 */
	public static void main(String[] args) throws IOException {
		JsonInfo jsoninfo = new JsonInfo();
		OlsrDataDump dump = jsoninfo.all();
		System.out.println("gateways:");
		for (Gateway g : dump.gateways)
			System.out.println("\t" + g.ipAddress);
		System.out.println("hna:");
		for (HNA h : dump.hna)
			System.out.println("\t" + h.destination);
		System.out.println("Interfaces:");
		for (Interface i : dump.interfaces)
			System.out.println("\t" + i.name);
		System.out.println("Links:");
		for (Link l : dump.links)
			System.out.println("\t" + l.localIP + " <--> " + l.remoteIP);
		System.out.println("MID:");
		for (MID m : dump.mid)
			System.out.println("\t" + m.ipAddress);
		System.out.println("Neighbors:");
		for (Neighbor n : dump.neighbors)
			System.out.println("\t" + n.ipv4Address);
		System.out.println("Plugins:");
		for (Plugin p : dump.plugins)
			System.out.println("\t" + p.plugin);
		System.out.println("Routes:");
		for (Route r : dump.routes)
			System.out.println("\t" + r.destination);
		System.out.println("Topology:");
		for (Node node : dump.topology)
			System.out.println("\t" + node.destinationIP);
	}
}
