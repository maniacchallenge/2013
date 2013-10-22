package de.fu_berlin.maniac.general;

import java.util.List;

import de.fu_berlin.maniac.exception.ManiacException;
import de.fu_berlin.maniac.packet_builder.*;

/**
 * 
 * @author ponken
 * 
 */
public interface ManiacStrategyInterface {

	/**
	 * This function is called by the Mothership class when a new advert is
	 * received.
	 * 
	 * It is advised that if you want to make any time and resource consuming
	 * action to NOT do them in this function because it will delay new incoming
	 * packets. You should do any time consuming actions in a seperate thread!
	 * 
	 * @param adv
	 *            All received adverts are passed to you using this function,
	 *            EXCEPT: 1. Adverts that advertise a data packet that this node
	 *            has already won previously. 2. Adverts that are from backbones
	 *            that are not this nodes associated backbone.
	 * 
	 * @return The return value should be a Long between 0 and AUCTION_TIMEOUT
	 *         (specified in Mothership). The return value determines after how
	 *         many milliseconds the sendBid(Advert adv) function is called.
	 *         This gives you the opportunity to delay your bid in case you want
	 *         to listen to other incoming bids before! If you return null or
	 *         anything <=0 there will be 0ms delay, if you return a value more
	 *         than AUCTION_TIMEOUT the delay will be AUCTION_TIMEOUT which will
	 *         most likely result in your bid not making it in time to the
	 *         advertising node (because of wireless travel time)
	 */
	public Long onRcvAdvert(Advert adv);

	/**
	 * This function is called after the delay that was returned with
	 * onRcvAdvert(Advert adv)
	 * 
	 * It is advised that if you want to make any time and resource consuming
	 * action to NOT do them in this function because it will delay new incoming
	 * packets. You should do any time consuming actions in a seperate thread!
	 * 
	 * NOTE: If the Integer returned is less than 0 or more than the MaxBid or
	 * null in the Advert message the node will bid for MaxBid.
	 * 
	 * @param adv
	 *            The same advert that was passed to onRcvAdvert(Advert adv)
	 *            before.
	 * @return The return value specifies your bid for the data packet
	 *         advertised by the node that sent the advert. NOTE: If the return
	 *         value is null, 0, or greater than the advertised maximum budget
	 *         in the advert (also referred to as maxBid or Ceil sometimes), the
	 *         maximum budget will be bid!
	 */
	public Integer sendBid(Advert adv);

	/**
	 * This function is called for every bid that is received so you can log it
	 * for your strategy It is advised that if you want to make any time and
	 * resource consuming action to NOT do them in this function because it will
	 * delay new incoming packets. You should do any time consuming actions in a
	 * seperate thread!
	 * 
	 * @param bid
	 *            This function will be called with every bid received (except
	 *            the bids this node sent out itself of course)
	 * 
	 */
	public void onRcvBid(Bid bid);

	/**
	 * This function is called for every bidwin (a packet announcing the winner
	 * of an auction, sent by the auctioneer) that is received so you can log it
	 * for your strategy. It is advised that if you want to make any time and
	 * resource consuming action to NOT do them in this function because it will
	 * delay new incoming packets. You should do any time consuming actions in a
	 * seperate thread!
	 * 
	 * @param bidwin
	 *            This function will be called with every bidwin received
	 *            (except the bids this node sent out itself of course)
	 */
	public void onRcvBidWin(BidWin bidwin);

	/**
	 * This function is called when a Data packet is received following an
	 * auction that this node has won. It is advised that if you want to make
	 * any time and resource consuming action to NOT do them in this function
	 * because it will delay new incoming packets. You should do any time
	 * consuming actions in a seperate thread!
	 * 
	 * @param packet
	 *            This function will be called with every data packet received
	 *            (which means that the data packet was won earlier by this node
	 *            in an auction)
	 * @return The return value is a AuctionParameters object, which contains
	 *         the two Integers 'maxBid' and 'fine'. If you return null, the
	 *         node will start an auction for this data packet with the same
	 *         parameters as the auction it was won from had, else it will start
	 *         an auction with the specified parameters (if they are valid, see
	 *         AuctionParamters)
	 */
	public AuctionParameters onRcvData(Data packet);

	/**
	 * This function is called at the end of an auction of a data packet
	 * perfomed by this node.
	 * 
	 * @param bids
	 *            It is passed a list of bids that came in for this data packet.
	 * @return The return value is the bid you choose to be the winner (and
	 *         which will get the Data packet). ************** IMPORTANT NOTE:
	 *         To use the backbone to deliver the data, return null. This will
	 *         send the data packet to the backbone, this will guarantee that
	 *         the packet will arrive at the finalDestination. This will cost as
	 *         much currency as the original maxBid advertised from originating
	 *         backbone was. **************
	 */
	public Bid selectWinner(List<Bid> bids);

	
	/**
	 * This function is called for every data packet received BEFORE the packet is processed for auction. It allows you to
	 * prevent the data packet from being processed at all. 
	 * IMPORTANT: 
	 * This function is called even if the data packet is addressed to the backbone
	 * associated with this node and thus could be delivered directly. So if you plan on
	 * dropping packets (by returning true) be aware of this and make sure you
	 * are not accidentally dropping packets that you would want to be delivered
	 * (unless of course, that is part of your strategy). To get the IP address
	 * of the backbone this node is associated with currently, call
	 * getMyOwnBackbone() in the NetworkManager (obtain instance first with
	 * getInstance())
	 * 
	 * Also, dropping the packet results in onRcvData not being called and
	 * subsequently no auction being started.
	 * 
	 * If you are unsure what to do with this function, just return false and
	 * every data packet will be processed as usual.
	 * 
	 * @param buffer_data A data packet that was just received
	 * @return Return true to drop the packet, false to have it processed
	 *         normally.
	 */
	public boolean dropPacketBefore(Data buffer_data);
	
	/**
	 * This function is called for every data packet that was auctioned by this node. The call occurs AFTER the auction but BEFORE
	 * a winner is selected and the data is sent out. That means this function allows you to auction off a packet (by returning false for dropPacketBefore()),
	 * watch the incoming bids come in and then decide whether to actually pick a winner and forward the data or to drop the data.
	 * Note that this function is not called if the data packet is addressed to the backbone associated with this node. In that case it
	 * will be delivered to its destination. To prevent that, see dropPacketBefore()
	 * @param buffer_data A received data packet that was not dropped with dropPacketBefore() and is not destined for the backbone associated
	 * with this node
	 * @return Return true to drop and false to not drop.
	 */
	public boolean dropPacketAfter(Data buffer_data);
	
	/**
	 * This function is called when an exception occurs in the ManiacLib.
	 * 
	 * @param ex
	 *            The exception that occurred in the ManiacLib
	 * @param fatal
	 *            Indicates whether the error was fatal and the ManiacLib needs
	 *            to be restarted
	 */
	public void onException(ManiacException ex, boolean fatal);


}
