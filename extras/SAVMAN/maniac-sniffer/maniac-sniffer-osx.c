/**
 * maniac packet sniffer
 * @author: Asanga Udugama (adu@comnets.uni-bremen.de)
 */
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netinet/ip.h>
#include <netinet/udp.h>
#include <netinet/if_ether.h>
#include <pcap.h>

#define ADVERT_CODE	'A'
#define BID_CODE	'B'
#define BIDWIN_CODE	'W'
#define DATA_CODE	'D'
#define MAX_NODES	24
#define MAX_TX		64

#pragma pack(1)
struct advert
{
	unsigned char type;
	uint32_t tx_id;
	struct in_addr final_dst_ipaddr;
	uint32_t ceil;
	uint32_t deadline;
	uint32_t fine;
};

#pragma pack(1)
struct bid
{
	unsigned char type;
	uint32_t tx_id;
	uint32_t bidv;
	struct in_addr final_dst_ipaddr;
};

#pragma pack(1)
struct bidwin
{
	unsigned char type;
	uint32_t tx_id;
	struct in_addr winner_ipaddr;
	uint32_t winbid;
	uint32_t fine;
};

#pragma pack(1)
struct data
{
	unsigned char type;
	uint32_t tx_id;
	struct in_addr final_dst_ipaddr;
	uint32_t hopcount;
	uint32_t datalen;
	uint32_t fine;
};

struct nodestats
{
	char node_ipaddr[24];
	int auctions_started;
	int bids_offered;
	int auctions_won;
	int data_sent;
	int balance;
};


struct tx_trace
{
	uint32_t tx_id;
	char final_dest_ipaddr[24];
	struct tx_bids
	{
		char winner_ipaddr[24];
		int winbid;
		int fine;
	} tx_bids_list[MAX_NODES];
	int tx_bids_list_count;
};

void pkt_handler(u_char *passed_var_ptr, const struct pcap_pkthdr *header, const u_char *pkt_data);

pcap_t *cap_hndl;
char pcap_errbuf[PCAP_ERRBUF_SIZE];
int link_type;
u_char passed_var;
int link_types_offset[] = {0, 12, -1, -1, -1, -1, 20, -1, -1, 2, 19, 6, -1 };
int data_start_offset[] = {4, 14, -1, -1, -1, -1, 22, -1, 16, 4, 21, 8,  0 };

struct nodestats nodestat_list[MAX_NODES];
int nodestat_list_max = 0;
int print_counter = 0;
char ifc_name[32];

int total_auctions_started = 0;
int total_bids_offered = 0;
int total_auctions_won = 0;
int total_data_sent = 0;

struct tx_trace tx_trace_list[MAX_TX];
int tx_trace_list_count = 0;

int main(int argc, char *argv[])
{
	int pkt_cnt;
	
	if(argc != 2) {
		printf("%s ifc\n", argv[0]);
		exit(-1);
	}

	strcpy(ifc_name, argv[1]);
	cap_hndl = pcap_open_live(ifc_name, 2048, 1, 20, pcap_errbuf);
	if(cap_hndl == NULL)
	{
		printf("Something is wrong: %s \n", pcap_errbuf);
		exit(1);
	}
	link_type = pcap_datalink(cap_hndl);
	printf("link type=%d", link_type);
	
	pkt_cnt = pcap_loop(cap_hndl, -1, (pcap_handler) pkt_handler, &passed_var);
}

void pkt_handler(u_char *passed_var_ptr, const struct pcap_pkthdr *header, const u_char *pkt_data)
{
	u_short link_proto;
	int i, j, k, found;
	struct ip *iph;
	struct udphdr *udph;
	char *mpktp;
	struct advert *advp;
	struct bid *bidp;
	struct bidwin *bidwinp;
	struct data *datap;
	struct in_addr saddr, daddr;
	
	// get link protocol
	link_proto = ntohs(*((u_short *) (pkt_data + link_types_offset[link_type])));
	
	// check the type - only IPv4
	if(link_proto != ETHERTYPE_IP) {
		return;
	}
	
	iph = (struct ip *) (pkt_data + data_start_offset[link_type]);
	
	// check whether UDP packet
	if(iph->ip_p != IPPROTO_UDP)
		return;
	
	// check whether MANIAC packet
	udph = (struct udphdr *) ((pkt_data + data_start_offset[link_type]) + sizeof(struct ip));
	if(ntohs(udph->uh_dport) != 8765)
		return;
	
	// print MANIAC packet contents
	saddr = iph->ip_src;
	daddr = iph->ip_dst;
	
	printf("From: %s, ", inet_ntoa(saddr));
	printf("To: %s\n", inet_ntoa(daddr));
	
	//for(i = 0; i < nodestat_list_max; i++) {
	//
	//}
	
	mpktp = (char *) ((pkt_data + data_start_offset[link_type]) + sizeof(struct ip) + sizeof(struct udphdr));
	
	if(*mpktp == ADVERT_CODE) {
		advp = (struct advert *) mpktp;
		
		printf("  Type: ADVERT, Tx: %d, Dest: %s, Ceil: %d, Dead: %d, Fine: %d \n",
			   ntohl(advp->tx_id), inet_ntoa(advp->final_dst_ipaddr),
			   ntohl(advp->ceil), ntohl(advp->deadline), ntohl(advp->fine));
		
		// update stats
		found = 0;
		for(i = 0; i < nodestat_list_max; i++) {
			if(strcmp(nodestat_list[i].node_ipaddr, inet_ntoa(saddr)) == 0) {
				found = 1;
				break;
			}
		}
		if(!found) {
			nodestat_list_max++;
			i = nodestat_list_max - 1;
			strcpy(nodestat_list[i].node_ipaddr, inet_ntoa(saddr));
			nodestat_list[i].auctions_started = 0;
			nodestat_list[i].bids_offered = 0;
			nodestat_list[i].auctions_won = 0;
			nodestat_list[i].data_sent = 0;
			nodestat_list[i].balance = 0;
		}
		nodestat_list[i].auctions_started++;
		total_auctions_started++;
		
	}
	else if(*mpktp == BID_CODE) {
		bidp = (struct bid *) mpktp;
		
		printf("  Type: BID, Tx: %d, Bid: %d, Dest: %s \n",
			   ntohl(bidp->tx_id), ntohl(bidp->bidv),
			   inet_ntoa(bidp->final_dst_ipaddr));

		// update stats
		found = 0;
		for(i = 0; i < nodestat_list_max; i++) {
			if(strcmp(nodestat_list[i].node_ipaddr, inet_ntoa(saddr)) == 0) {
				found = 1;
				break;
			}
		}
		if(!found) {
			nodestat_list_max++;
			i = nodestat_list_max - 1;
			strcpy(nodestat_list[i].node_ipaddr, inet_ntoa(saddr));
			nodestat_list[i].auctions_started = 0;
			nodestat_list[i].bids_offered = 0;
			nodestat_list[i].auctions_won = 0;
			nodestat_list[i].data_sent = 0;
			nodestat_list[i].balance = 0;
		}
		nodestat_list[i].bids_offered++;
		total_bids_offered++;
		
	}
	else if(*mpktp == BIDWIN_CODE) {
		bidwinp = (struct bidwin *) mpktp;
		
		printf("  Type: BIDWIN, Tx: %d, Winner: %s, Bid: %d, Fine: %d \n",
			   ntohl(bidwinp->tx_id), inet_ntoa(bidwinp->winner_ipaddr),
			   ntohl(bidwinp->winbid), ntohl(bidwinp->fine));

		// update stats
		found = 0;
		for(i = 0; i < nodestat_list_max; i++) {
			if(strcmp(nodestat_list[i].node_ipaddr, inet_ntoa(bidwinp->winner_ipaddr)) == 0) 				{
				found = 1;
				break;
			}
		}
		if(!found) {
			nodestat_list_max++;
			i = nodestat_list_max - 1;
			strcpy(nodestat_list[i].node_ipaddr, inet_ntoa(bidwinp->winner_ipaddr));
			nodestat_list[i].auctions_started = 0;
			nodestat_list[i].bids_offered = 0;
			nodestat_list[i].auctions_won = 0;
			nodestat_list[i].data_sent = 0;
			nodestat_list[i].balance = 0;
		}
		nodestat_list[i].auctions_won++;
		total_auctions_won++;
		
		// update win list of tx
		found = 0;
		for(i = 0; i < tx_trace_list_count; i++) {
			if(tx_trace_list[i].tx_id == ntohl(bidwinp->tx_id)) {
				found = 1;
				break;
			}
		}
		if(!found) {
			tx_trace_list_count++;
			i = tx_trace_list_count - 1;
			tx_trace_list[i].tx_id = ntohl(bidwinp->tx_id);
			strcpy(tx_trace_list[i].final_dest_ipaddr, inet_ntoa(bidwinp->winner_ipaddr));
			tx_trace_list[i].tx_bids_list_count = 0;
		}
		tx_trace_list[i].tx_bids_list_count++;
		j = tx_trace_list[i].tx_bids_list_count - 1;
		strcpy(tx_trace_list[i].tx_bids_list[j].winner_ipaddr, inet_ntoa(bidwinp->winner_ipaddr));
		tx_trace_list[i].tx_bids_list[j].winbid = ntohl(bidwinp->winbid);
		tx_trace_list[i].tx_bids_list[j].fine = ntohl(bidwinp->fine);
		
		for(i = 0; i < nodestat_list_max; i++) {
			if(strcmp(nodestat_list[i].node_ipaddr, inet_ntoa(bidwinp->winner_ipaddr)) == 0) {
				nodestat_list[i].balance -= ntohl(bidwinp->fine);
				break;
			}
		}
		
	}
	else if(*mpktp == DATA_CODE) {
		datap = (struct data *) mpktp;
		
		printf("  Type: DATA, Tx: %d, Dest: %s, Hop Count: %d, Data Len: %d, Fine: %d \n",
			   ntohl(datap->tx_id), inet_ntoa(datap->final_dst_ipaddr),
			   ntohl(datap->hopcount), ntohl(datap->datalen), ntohl(advp->fine));
		
		// update stats
		found = 0;
		for(i = 0; i < nodestat_list_max; i++) {
			if(strcmp(nodestat_list[i].node_ipaddr, inet_ntoa(saddr)) == 0) 				{
				found = 1;
				break;
			}
		}
		if(!found) {
			nodestat_list_max++;
			i = nodestat_list_max - 1;
			strcpy(nodestat_list[i].node_ipaddr, inet_ntoa(saddr));
			nodestat_list[i].auctions_started = 0;
			nodestat_list[i].bids_offered = 0;
			nodestat_list[i].auctions_won = 0;
			nodestat_list[i].data_sent = 0;
			nodestat_list[i].balance = 0;
		}
		nodestat_list[i].data_sent++;
		total_data_sent++;

		// check final data delivery and adjust balance
		found = 0;
		for(i = 0; i < tx_trace_list_count; i++) {
			if(tx_trace_list[i].tx_id == ntohl(datap->tx_id)) {
				found = 1;
				break;
			}
		}
		if(found && strcmp(inet_ntoa(daddr), tx_trace_list[i].final_dest_ipaddr) == 0) {
			for (j = 0; j < tx_trace_list[i].tx_bids_list_count; j++) {
				for(k = 0; k < nodestat_list_max; k++) {
					if(strcmp(nodestat_list[k].node_ipaddr, tx_trace_list[i].tx_bids_list[j].winner_ipaddr) == 0) {
						nodestat_list[k].balance += tx_trace_list[i].tx_bids_list[j].fine;
						nodestat_list[k].balance += tx_trace_list[i].tx_bids_list[j].winbid;
					}
				}
			}
		}
	}
	else {
		printf("Unknown MANIAC packet found, exiting");
		exit(-1);
	}
	printf("\n");
	
	if(print_counter > 10) {
		printf("======Current Stats======\n");
		for(i = 0; i < nodestat_list_max; i++) {
		
			printf("Node: %s, Auctions Made: %d, Bids Offered: %d, Auctions Won: %d Data Sent: %d, Balance: %d \n",
					nodestat_list[i].node_ipaddr,
					nodestat_list[i].auctions_started,
					nodestat_list[i].bids_offered,
					nodestat_list[i].auctions_won,
					nodestat_list[i].data_sent,
					nodestat_list[i].balance);
		}
		printf("Total, Auctions Made: %d, Bids Offered: %d, Auctions Won: %d Data Sent: %d \n",
					total_auctions_started,
					total_bids_offered,
					total_auctions_won,
					total_data_sent);
		printf("=========================\n");
		printf("\n");
		print_counter = 0;
	}
	print_counter++;
	
}




