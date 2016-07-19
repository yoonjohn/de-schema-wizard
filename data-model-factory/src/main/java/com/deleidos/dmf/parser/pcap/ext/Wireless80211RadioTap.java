package com.deleidos.dmf.parser.pcap.ext;

import java.nio.ByteOrder;

import org.jnetpcap.PcapDLT;
import org.jnetpcap.nio.JBuffer;
import org.jnetpcap.packet.JHeader;
import org.jnetpcap.packet.JPacket;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.annotate.Bind;
import org.jnetpcap.packet.annotate.Field;
import org.jnetpcap.packet.annotate.Header;
import org.jnetpcap.packet.annotate.Header.Characteristic;
import org.jnetpcap.packet.annotate.Header.Layer;
import org.jnetpcap.packet.annotate.HeaderLength;
import org.jnetpcap.packet.annotate.ProtocolSuite;

@Header(nicname = "802.11 MAC RadioTap", dlt = {
		PcapDLT.IEEE802_11_RADIO
}, suite = ProtocolSuite.WIRELESS, osi = Layer.DATALINK, characteristics = Characteristic.CSMA_CD, description = "Wifi Radiotap")
public class Wireless80211RadioTap extends JHeader {

	@HeaderLength
	public static int headerLength(JBuffer packet, int offset) {
		if(packet.order().equals(ByteOrder.BIG_ENDIAN)) {
			int first = (packet.getUByte(2) << 8);
			int second = packet.getUByte(3);
			int length = (first | second);
			return length;
		} else {
			int first = packet.getUByte(2);
			int second = packet.getUByte(3) << 8;
			int length = (first | second);
			return length;
		}
	}

}
