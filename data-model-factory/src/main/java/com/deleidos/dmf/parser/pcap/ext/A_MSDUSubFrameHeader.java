package com.deleidos.dmf.parser.pcap.ext;

import java.nio.ByteOrder;

import org.jnetpcap.PcapDLT;
import org.jnetpcap.packet.JHeader;
import org.jnetpcap.packet.annotate.Field;
import org.jnetpcap.packet.annotate.Header;
import org.jnetpcap.packet.annotate.HeaderLength;
import org.jnetpcap.packet.annotate.ProtocolSuite;

import com.deleidos.dmf.parser.JNetPcapTikaParser;

import org.jnetpcap.packet.annotate.Header.Characteristic;
import org.jnetpcap.packet.annotate.Header.Layer;

@Header(nicname = "802.11 MAC", 
suite = ProtocolSuite.WIRELESS, osi = Layer.DATALINK, length=14,
characteristics = Characteristic.CSMA_CD, description = "A-MSDU Subframe")
public class A_MSDUSubFrameHeader extends JHeader {
	
	@Field(offset=0, length=48, description="DA")
	public byte[] da() {
		byte[] b = super.getByteArray(0, 6);
		if(super.order().equals(ByteOrder.BIG_ENDIAN)) {
			return b;
		} else {
			return JNetPcapTikaParser.swap(b);
		}
	}
	
	@Field(offset=48, length=48, description="SA")
	public byte[] sa() {
		byte[] b = super.getByteArray(6, 6);
		if(super.order().equals(ByteOrder.BIG_ENDIAN)) {
			return b;
		} else {
			return JNetPcapTikaParser.swap(b);
		}
	}
	
	@Field(offset=96, length=16, description="Length")
	public int length() {
		byte[] b = super.getByteArray(12, 2);
		if(super.order().equals(ByteOrder.BIG_ENDIAN)) {
			return (b[0] << 8) | (b[1]);
		} else {
			b = JNetPcapTikaParser.swap(b);
			return (b[0] << 8) | (b[1]);
		}
	}
}
