package de.fu_berlin.maniac.packet_builder;

import java.net.DatagramPacket;

import de.fu_berlin.maniac.packet_builder.ProtoPackets.GeneralPurposeMessage;
import de.fu_berlin.maniac.packet_builder.ProtoPackets.PacketMessage;

public class GeneralPurposePacket extends Packet {
	
	private String message = "";
	private int transactionID = 0;
	
	public GeneralPurposePacket(PacketMessage packetMessage){
		parse(packetMessage);
	}
	
	public GeneralPurposePacket(String message){
		this.message = message;
	}
	
	public String getMessage(){
		return this.message;
	}
	
	@Override
	protected DatagramPacket getDatagramPacket() {
		byte[] payload = buildPayload();
		return new DatagramPacket(payload, payload.length, this.destinationIP,
				PACKET_PORT);
	}
	
	private byte[] buildPayload(){
		GeneralPurposeMessage gpm = GeneralPurposeMessage.newBuilder()
				.setMessage(this.message)
				.build();
		
		PacketMessage packetMessage = PacketMessage.newBuilder()
				.setType(PacketMessage.packetType.GENERALPURPOSE)
				.setTransactionID(this.transactionID)
				.setGeneralPurposeMessage(gpm)
				.build();
		
		return packetMessage.toByteArray();
	}
	
	private void parse(PacketMessage packetMessage ){
		GeneralPurposeMessage gpm = packetMessage.getGeneralPurposeMessage();
		this.message = gpm.getMessage();
	}
}
