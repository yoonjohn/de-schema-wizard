package com.deleidos.dmf.parser.pcap.ext;

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jnetpcap.PcapDLT;
import org.jnetpcap.nio.JBuffer;
import org.jnetpcap.packet.JHeader;
import org.jnetpcap.packet.JPacket;
import org.jnetpcap.packet.annotate.Bind;
import org.jnetpcap.packet.annotate.Dynamic;
import org.jnetpcap.packet.annotate.Field;
import org.jnetpcap.packet.annotate.Header;
import org.jnetpcap.packet.annotate.Header.Characteristic;
import org.jnetpcap.packet.annotate.Header.Layer;
import org.jnetpcap.packet.annotate.HeaderLength;
import org.jnetpcap.packet.annotate.ProtocolSuite;

import com.deleidos.dmf.parser.JNetPcapTikaParser;

@Header(nicname = "802.11 MAC", dlt = {
		PcapDLT.IEEE802_11
}, suite = ProtocolSuite.WIRELESS, osi = Layer.DATALINK, characteristics = Characteristic.CSMA_CD, description = "Wifi")
public class Wireless80211 extends JHeader {
	private static final Logger logger = Logger.getLogger(Wireless80211.class);
	public static Map<Integer, InformationElementExtractor> informationElementExtractors = new HashMap<Integer, InformationElementExtractor>();

	public static final int DATA_SHORT_HDR_LEN = 24;
	public static final int DATA_LONG_HDR_LEN = 30;
	public static final int MANAGEMENT_FRAME_LEN = 24;
	public static final int HT_CONTROL_LEN = 4;
	public static final int RTS_FRAME_LEN = 16;
	public static final int CTS_FRAME_LEN = 10;
	public static final int ACK_FRAME_LEN = 10;
	public static final int PS_POLL_FRAME_LEN = 16;
	public static final int CF_END_FRAME_LEN = 16;
	public static final int CF_END_CF_ACK_FRAME_LEN = 16;
	public static final int BLOCK_ACK_REQ_frame_LEN = 16;
	public static final int CONTROL_WRAPPER_LENGTH = 16;

	public static final int VERSION = 0b00;

	// types
	public static final int MANAGEMENT_FRAME = 0b00;
	public static final int CONTROL_FRAME = 0b01;
	public static final int DATA_FRAME = 0b10;
	public static final int RESERVED = 0b11;

	// management frame subtypes
	public static final int ASSOCIATION_REQUEST = 0b0000;
	public static final int ASSOCIATION_RESPONSE = 0b0001;
	public static final int REASSOCIATION_REQUEST = 0b0010;
	public static final int REASSOCIATION_RESPONSE = 0b0011;
	public static final int PROBE_REQUEST = 0b0100;
	public static final int PROBE_RESPONSE = 0b0101;
	public static final int TIMING_ADVERTISEMENT = 0b0110;
	public static final int BEACON = 0b1000;
	public static final int ATIM = 0b1001;
	public static final int DISASSOCIATION = 0b1010;
	public static final int AUTHENTICATION = 0b1011;
	public static final int DEAUTHENTICATION = 0b1100;
	public static final int ACTION = 0b1101;
	public static final int ACTION_NO_ACK = 0b1110;

	// control frame subtypes
	public static final int CONTROL_WRAPPER = 0b0111;
	public static final int BLOCK_ACK_REQUEST = 0b1000;
	public static final int BLOCK_ACK = 0b1001;
	public static final int PS_POLL = 0b1010;
	public static final int RTS = 0b1011;
	public static final int CTS = 0b1100;
	public static final int ACK = 0b1101;
	public static final int CF_END = 0b1110;
	public static final int CF_END_CF_ACK = 0b1111;

	// data frame subtypes
	public static final int DATA = 0b0000;
	public static final int DATA_CF_ACK = 0b0001;
	public static final int DATA_CF_POLL = 0b0010;
	public static final int DATA_CF_ACK_CF_POLL = 0b0011;
	public static final int NULL = 0b0100;
	public static final int CF_ACK_NO_DATA = 0b0101;
	public static final int CF_POLL_NO_DATA = 0b0110;
	public static final int CF_ACK_CF_POLL_NO_DATA = 0b0111;
	public static final int QOS_DATA = 0b1000;
	public static final int QOS_DATA_CF_ACK = 0b1001;
	public static final int QOS_DATA_CF_POLL = 0b1010;
	public static final int QOS_DATA_CF_ACK_CF_POLL = 0b1011;
	public static final int QOS_NULL_NO_DATA = 0b1100;
	public static final int QOS_CF_POLL_NO_DATA = 0b1110;
	public static final int QOS_CF_ACK_CF_POLL_NO_DATA = 0b1111;

	private Map<Integer, String> typeMap = new HashMap<Integer, String>();
	private Map<Integer, String> subtypeMap = new HashMap<Integer, String>();

	private int type;
	private int subtype;
	private int frameControlField;
	private int duration;
	private byte[] address1;
	private byte[] address2;
	private byte[] address3;
	private byte[] address4;
	private int sequenceNumber;
	private int fragmentNumber;
	private int qosControl;
	private int htControl;
	private Map<String, Object> informationElements;

	public Wireless80211() {
		setup();
	}

	/**
	 * <a href>http://stackoverflow.com/questions/12407145/interpreting-frame-control-bytes-in-802-11-wireshark-trace</a>
	 */
	private void setup() {
		typeMap.put(DATA_FRAME, "Data Frame");
		typeMap.put(MANAGEMENT_FRAME, "Management Frame");
		typeMap.put(CONTROL_FRAME, "Control Frame");
		typeMap.put(RESERVED, "Reserved");
		subtypeMap.put(PROBE_REQUEST, "Probe Request");
		subtypeMap.put(BEACON, "Beacon");
		subtypeMap.put(PROBE_RESPONSE, "Probe Response");
		subtypeMap.put(NULL, "Null");
		subtypeMap.put(ACK, "Ack");
		subtypeMap.put(DATA, "Data");
		subtypeMap.put(QOS_DATA, "QoS Data");
		subtypeMap.put(CTS, "CTS");
		subtypeMap.put(RTS, "RTS");
		subtypeMap.put(CF_END, "CF-End");
		subtypeMap.put(CF_END_CF_ACK, "CF-End+CF-Ack");
		subtypeMap.put(PS_POLL, "PS-Poll");
	}

	public String typeString(int type) {
		return typeMap.get(type);
	}

	public String subtypeString(int subtype) {
		return subtypeMap.get(subtype);
	}

	/**
	 * Not ready
	 * @return
	 */
	public int frameCheckSequence() {
		byte[] bytes = super.getByteArray(super.size()-4, 4);
		if(super.order().equals(ByteOrder.BIG_ENDIAN)) {
			int fcs = (bytes[0] << 24) & (bytes[1] << 16) & (bytes[2] << 8) & (bytes[3]);
			return fcs;
		} else {
			int fcs = (bytes[3] << 24) & (bytes[2] << 16) & (bytes[1] << 8) & (bytes[0]);
			return fcs;
		}
	}

	@HeaderLength
	public static int headerLength(JBuffer buffer, int offset) {
		byte[] b = buffer.getByteArray(offset, 2);
		/*
		 * order() calls seem to be inconsistent*/
		int length = 0;
		/*if(ByteOrder.nativeOrder().equals(buffer.order().equals(ByteOrder.LITTLE_ENDIAN))) {
			b = JNetPcapTikaParser.swap(b);
		}*/
		int type = (b[0] >> 2) & 0b11;
		int subtype = (b[0] >> 4) & 0b1111;
		int order = (b[1] & 0b1);
		boolean qosDataOrManagementFrame = 
				(type == DATA && subtype >= QOS_DATA) || (type == MANAGEMENT_FRAME);
		if(order == 1 && qosDataOrManagementFrame) {
			length = 4;
		}
		if(type == CONTROL_FRAME) {
			switch(subtype) {
			case ACK: {
				return ACK_FRAME_LEN;
			}
			case CTS: {
				return CTS_FRAME_LEN;
			}
			case RTS: {
				return RTS_FRAME_LEN;
			}
			case CF_END: {
				return CF_END_FRAME_LEN;
			}
			case CF_END_CF_ACK: {
				return CF_END_CF_ACK_FRAME_LEN;
			}
			case PS_POLL: {
				return PS_POLL_FRAME_LEN;
			}
			case CONTROL_WRAPPER: {
				return CONTROL_WRAPPER_LENGTH;
			}
			default: {
				return 0;
			}
			}
		}
		int secondByte = Byte.toUnsignedInt(b[1]);
		//length +=
		int toAndFrom = analyzeToAndFromDS(secondByte);
		switch(toAndFrom) {
		case 0b00: {
			length += MANAGEMENT_FRAME_LEN;
			break;
		}
		case 0b01: {
			length += DATA_SHORT_HDR_LEN;
			break;
		}
		case 0b10: {
			length += DATA_SHORT_HDR_LEN;
			break;
		}
		case 0b11: {
			length += DATA_LONG_HDR_LEN;
			break;
		}
		}
		return length;
	}
	
	private static int analyzeToAndFromDS(int secondFRCByte) {
		int toDS = (secondFRCByte >> 3) & 0b1;
		int fromDS = (secondFRCByte >> 2) & 0b1;
		return toDS << 1 | fromDS;
		/*if(toDS == 0 && fromDS == 0) {
			return 0b00;
		} else if(toDS == 0 && fromDS == 1) {
			return 0b01;
		} else if(toDS == 1 && fromDS == 0) {
			return 0b10;
		} else if(toDS == 1 && fromDS == 1) {
			return 0b11;
		} else {
			return -1;
		}*/
	}

	private void calculateType(int frameControlField) {
		this.type = (frameControlField >> 10) & 0b11;
	}

	@Field(offset=2, length=2, description="type")
	public int type() {
		return type;
	}

	private void calculateSubType(int frameControlField) {
		this.subtype = (frameControlField >> 12) & 0b1111;
	}

	@Field(offset=4, length=4, description="subtype")
	public int subtype() {
		return subtype;
	}

	private void calculateFrameControlField(JHeader header) {
		byte[] b = header.getByteArray(0, 2);
		if(header.order().equals(ByteOrder.BIG_ENDIAN)) {
			this.frameControlField = (Byte.toUnsignedInt(b[0]) << 8) | b[1];
		} else {
			b = JNetPcapTikaParser.swap(b);
			this.frameControlField = (Byte.toUnsignedInt(b[0]) << 8) | b[1];
		}
	}

	@Field(offset=0, length = 16, description="frame control flags") 
	public int frameControlField() {
		return frameControlField;
	}

	@Override
	protected void decodeHeader() {
		super.decodeHeader();
		reinitialize();
		calculateFrameControlField(this);
		calculateType(this.frameControlField);
		calculateSubType(this.frameControlField);
		byte[] d = super.getByteArray(2, 2);
		duration = Byte.toUnsignedInt(d[0]) | Byte.toUnsignedInt(d[1]) << 8;
		switch(type) {
		case MANAGEMENT_FRAME: {
			// general management frame header components
			address1 = super.getByteArray(4, 6);
			address2 = super.getByteArray(10, 6);
			address3 = super.getByteArray(16, 6);
			byte[] b = super.getByteArray(22, 2);
			int i = Byte.toUnsignedInt(b[0]) | Byte.toUnsignedInt(b[1]) << 8;
			sequenceNumber = (i & (0xfff<<4)) >> 4;
			fragmentNumber = i & 0xf;
			switch(subtype) {
			case ASSOCIATION_REQUEST: {
				break;
			}
			case ASSOCIATION_RESPONSE: {
				break;
			}
			case REASSOCIATION_REQUEST: {
				break;
			}
			case REASSOCIATION_RESPONSE: {
				break;
			}
			case PROBE_REQUEST: {
				break;
			}
			case PROBE_RESPONSE: {
				break;
			}
			case TIMING_ADVERTISEMENT: {
				break;
			}
			case BEACON: {
				break;
			}
			case ATIM: {
				break;
			}
			case DISASSOCIATION: {
				break;
			}
			case AUTHENTICATION: {
				break;
			}
			case DEAUTHENTICATION: {
				break;
			}
			case ACTION: {
				break;
			}
			case ACTION_NO_ACK: {
				break;
			}
			}
		}
		case CONTROL_FRAME: {
			// general control frame header components
			address1 = super.getByteArray(4, 6);
			switch(subtype) {
			case ACK: {
				return;
			}
			case CTS: {
				return;
			}
			case RTS: {
				address2 = super.getByteArray(10, 6);
				return;
			}
			case CF_END: {
				address2 = super.getByteArray(10, 6);
				return;
			}
			case CF_END_CF_ACK: {
				address2 = super.getByteArray(10, 6);
				return;
			}
			case PS_POLL: {
				address2 = super.getByteArray(10, 6);
				return;
			}
			}
		}
		case DATA_FRAME: {
			address1 = super.getByteArray(4, 6);
			address2 = super.getByteArray(10, 6);
			address3 = super.getByteArray(16, 6);
			byte[] b = super.getByteArray(22, 2);
			int i = Byte.toUnsignedInt(b[0]) | Byte.toUnsignedInt(b[1]) << 8;
			sequenceNumber = (i & (0xfff<<4)) >> 4;
			fragmentNumber = i & 0xf;
			int postStandardOffset = 0;
			if(0b11 == analyzeToAndFromDS(this.frameControlField & 0xff)) {
				address4 = super.getByteArray(24, 6);
				postStandardOffset += 6;
			}
			boolean isQos = type == DATA_FRAME && ((subtype & 0b1000) > 0);
			/* TODO can't quite get this working
			 * if(isQos) {
				byte[] qosArr = super.getByteArray(24+postStandardOffset, 2);
				qosControl = qosArr[0] | qosArr[1] << 8;
				postStandardOffset += 2;
			}*/
			int orderFlag = frameControlField & 0x1;
			boolean htControlPresent = isQos && orderFlag == 1;
			/*if(htControlPresent) {
				byte[] htControlArr = super.getByteArray(24+postStandardOffset, 4);
				htControl = Byte.toUnsignedInt(htControlArr[0]) 
						| Byte.toUnsignedInt(htControlArr[1]) << 8 
						| Byte.toUnsignedInt(htControlArr[2]) << 16
						| Byte.toUnsignedInt(htControlArr[3]) << 24;
			}*/
			switch (subtype) {
			case DATA: {
				break;
			}
			case DATA_CF_ACK: {
				break;
			}
			case DATA_CF_POLL: {
				break;
			}
			case DATA_CF_ACK_CF_POLL: {
				break;
			}
			case NULL: {
				break;
			}
			case CF_ACK_NO_DATA: {
				break;
			}
			case CF_POLL_NO_DATA: {
				break;
			}
			case CF_ACK_CF_POLL_NO_DATA: {
				break;
			}
			case QOS_DATA: {
				break;
			}
			case QOS_DATA_CF_ACK: {
				break;
			}
			case QOS_DATA_CF_POLL: {
				break;
			}
			case QOS_DATA_CF_ACK_CF_POLL: {
				break;
			}
			case QOS_NULL_NO_DATA: {
				break;
			}
			case QOS_CF_POLL_NO_DATA: {
				break;
			}
			case QOS_CF_ACK_CF_POLL_NO_DATA: {
				break;
			}
			}
		}
		case RESERVED: {
			break;
		}
		}
	}

	private void reinitialize() {
		frameControlField = -1;
		address1 = null;
		address2 = null;
		address3 = null;
		sequenceNumber = -1;
		fragmentNumber = -1;
		address4 = null;
		htControl = -1;
		informationElements = new HashMap<String, Object>();
	}

	public byte[] address1() {
		return address1;
	}

	public byte[] address2() {
		return address2;
	}

	public byte[] address3() {
		return address3;
	}
	
	public byte[] address4() {
		return address4;
	}

	public int seqNumber() {
		return sequenceNumber;
	}

	public int fragNumber() {
		return fragmentNumber;
	}
	
	public void addSuccessfullyParsedValuesToMap(Map<String, Object> map) {
		map.put("wlan.fc.type", typeString(type()));
		map.put("wlan.fc.subtype", subtypeString(subtype()));
		map.put("wlan.duration", duration);
		if(type() == MANAGEMENT_FRAME) {
			map.put("wlan.ra", JNetPcapTikaParser.macBytesToString(address1()));
			map.put("wlan.sa", JNetPcapTikaParser.macBytesToString(address2()));
			switch(subtype()) {
			case PROBE_REQUEST: {
				break;
			}
			case ACTION: {
				break;
			}
			}
		} else if(type() == CONTROL_FRAME) {
			switch(subtype) {
			case ACK: {
				map.put("wlan.ra", JNetPcapTikaParser.macBytesToString(address1()));
				break;
			}
			case CTS: {
				map.put("wlan.ra", JNetPcapTikaParser.macBytesToString(address1()));
				break;
			}
			case RTS: {
				map.put("wlan.ra", JNetPcapTikaParser.macBytesToString(address1()));
				map.put("wlan.ta", JNetPcapTikaParser.macBytesToString(address2()));
				break;
			}
			case CF_END: {
				break;
			}
			case CF_END_CF_ACK: {
				break;
			}
			case PS_POLL: {
				map.put("wlan.da", JNetPcapTikaParser.macBytesToString(address1()));
				map.put("wlan.sa", JNetPcapTikaParser.macBytesToString(address2()));
				break;
			}
			}
		} else if(type() == DATA_FRAME) {
			int toFromDS = analyzeToAndFromDS(frameControlField());
			// TODO need to determine QoS MSDU field
			switch(toFromDS) {
			case 0b00: {
				map.put("wlan.ra", JNetPcapTikaParser.macBytesToString(address1()));
				map.put("wlan.ta", JNetPcapTikaParser.macBytesToString(address2()));
				map.put("wlan.bssid", JNetPcapTikaParser.macBytesToString(address3()));
				break;
			}
			case 0b01: {
				map.put("wlan.ra", JNetPcapTikaParser.macBytesToString(address1()));
				map.put("wlan.bssid", JNetPcapTikaParser.macBytesToString(address2()));
				map.put("wlan.bssid", JNetPcapTikaParser.macBytesToString(address3()));
				break;
			}
			case 0b10: {
				map.put("wlan.ra", JNetPcapTikaParser.macBytesToString(address1()));
				map.put("wlan.ta", JNetPcapTikaParser.macBytesToString(address2()));
				map.put("wlan.bssid", JNetPcapTikaParser.macBytesToString(address3()));
				break;
			}
			case 0b11: {
				map.put("wlan.ra", JNetPcapTikaParser.macBytesToString(address1()));
				map.put("wlan.ta", JNetPcapTikaParser.macBytesToString(address2()));
				map.put("wlan.bssid", JNetPcapTikaParser.macBytesToString(address3()));
				map.put("wlan.sa", JNetPcapTikaParser.macBytesToString(address4()));
				break;
			}
			}
		}
		/*if(address1 != null) {
			map.put("wlan.ra", JNetPcapTikaParser.macBytesToString(address1()));
		}
		if(address2 != null) {
			map.put("wlan.ta", JNetPcapTikaParser.macBytesToString(address2()));
		}
		if(address3 != null) {
			map.put("wlan.bssid", JNetPcapTikaParser.macBytesToString(address3()));
		}
		if(address4 != null) {
			map.put("wlan.addr4", JNetPcapTikaParser.macBytesToString(address4()));
		}*/
		if(sequenceNumber > 0) {
			map.put("wlan.seq", seqNumber());
		}
		if(fragmentNumber > 0) {
			map.put("wlan.frag", fragNumber());
		}
		map.putAll(informationElements);
	}

	public Map<Integer, String> getTypeMap() {
		return typeMap;
	}

	public void setTypeMap(Map<Integer, String> typeMap) {
		this.typeMap = typeMap;
	}

	public Map<Integer, String> getSubtypeMap() {
		return subtypeMap;
	}

	public void setSubtypeMap(Map<Integer, String> subtypeMap) {
		this.subtypeMap = subtypeMap;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public int getFragmentNumber() {
		return fragmentNumber;
	}

	public void setFragmentNumber(int fragmentNumber) {
		this.fragmentNumber = fragmentNumber;
	}

	public byte[] getAddr1() {
		return address1;
	}

	public void setAddr1(byte[] addr1) {
		this.address1 = addr1;
	}

	public byte[] getAddr2() {
		return address2;
	}

	public void setAddr2(byte[] addr2) {
		this.address2 = addr2;
	}

	public byte[] getAddr3() {
		return address3;
	}

	public void setAddr3(byte[] addr3) {
		this.address3 = addr3;
	}

	public byte[] getAddr4() {
		return address4;
	}

	public void setAddr4(byte[] addr4) {
		this.address4 = addr4;
	}
	
	@Bind(to = Wireless80211RadioTap.class)
	public static boolean bindToRadioTap(JPacket packet, Wireless80211RadioTap wirelessRadioTap) {
		return true;
	}

	public static void addInformationElementExtractor(InformationElementExtractor extractor) {
		informationElementExtractors.put(extractor.elementId(), extractor);
	}
	
	public interface InformationElementExtractor {
		/**
		 * The element ID that will be extracted.
		 * @return
		 */
		public int elementId();
		/**
		 * Return any set of values that should be added to the data model from this information
		 * element.
		 * @param dataBytes The entire data portion of the wireless frame
		 * @param offset The offset at which the element was detected 
		 * @return the set of values to be added to the data model.
		 */
		public Map<String, Object> extractInformationElement(byte[] dataBytes, int offset);
	}
}
