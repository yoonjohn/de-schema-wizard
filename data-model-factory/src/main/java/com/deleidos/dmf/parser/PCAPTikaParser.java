package com.deleidos.dmf.parser;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.xml.sax.ContentHandler;
import com.deleidos.dmf.exception.AnalyticsTikaProfilingException;
import com.deleidos.dmf.framework.AbstractAnalyticsParser;
import com.deleidos.dmf.framework.TikaProfilerParameters;
import com.deleidos.dp.profiler.BinaryProfilerRecord;
import com.deleidos.dp.profiler.api.ProfilerRecord;
import com.deleidos.rtws.splitter.PcapSplitter;


public class PCAPTikaParser extends AbstractAnalyticsParser {
	private PcapSplitter pcapSplitter;
	private static final Set<MediaType> SUPPORTED_TYPES = 
			Collections.singleton(MediaType.application("vnd.tcpdump.pcap"));

	public static final String PCAP_TYPE = "application/vnd.tcpdump.pcap";


	@Override
	public Set<MediaType> getSupportedTypes(ParseContext context) {
		return SUPPORTED_TYPES;
	}
	
	@Override
	public void preParse(InputStream inputStream, ContentHandler handler, Metadata metadata,
			TikaProfilerParameters context) {
		pcapSplitter = new PcapSplitter();
		pcapSplitter.setInputStream(inputStream);
		pcapSplitter.parseHeaders();
	}
	
	@Override
	public ProfilerRecord getNextProfilerRecord(InputStream inputStream, ContentHandler handler, Metadata metadata,
			TikaProfilerParameters context) throws AnalyticsTikaProfilingException {
		String pcapSplit = pcapSplitter.split();
		if(pcapSplit == null) {
			return null;
		}
		BinaryProfilerRecord binaryRecord = new BinaryProfilerRecord("pcap", ByteBuffer.wrap(pcapSplit.getBytes()));
		return binaryRecord;
	}

	
	/*
	public JSONArray loadPcapToJson(String file) throws SAXException {
		StringBuilder sb = new StringBuilder();
		Pcap pcap = Pcap.openOffline(file, sb);
		if (pcap == null) {
			System.err.println("Error opening pcap file " + file + " with error: " + sb.toString());
			if(!streamCalled) System.err.println("Copy of stream was not created.");
			return null;
		}
		PcapPacketHandler<Queue<PcapPacket>> handler = new PcapPacketHandler<Queue<PcapPacket>>() {
			@Override
			public void nextPacket(PcapPacket packet, Queue<PcapPacket> queue) {
				PcapPacket permanent = new PcapPacket(packet);
				queue.offer(packet);
			}
		};
		Queue<PcapPacket> queue = new ArrayBlockingQueue<PcapPacket>(1000);
		pcap.loop(100, handler, queue);
		Tcp tcp = new Tcp();
		Ip4 ip = new Ip4();
		JSONArray array = new JSONArray();
		while(!queue.isEmpty()) {
			PcapPacket packet = queue.poll();
			JSONObject packetJson = new JSONObject();
			if(packet.hasHeader(ip)) {
				JSONObject ipHeader = new JSONObject();
				ipHeader.put("source ip", bytesToIPString(ip.source()));
				ipHeader.put("destination ip", bytesToIPString(ip.destination()));
				ipHeader.put("version", ip.version());
				ipHeader.put("length", ip.length());
				ipHeader.put("type", ip.type());
				ipHeader.put("checksum", ip.checksum());
				ipHeader.put("offset", ip.offset());
				ipHeader.put("ttl", ip.ttl());
				ipHeader.put("id",ip.id());
				ipHeader.put("isFragmented",ip.isFragmented());
				packetJson.put("ipHeader", ipHeader);
			}
			if(packet.hasHeader(tcp)) {
				JSONObject tcpHeader = new JSONObject();
				tcpHeader.put("source port", String.valueOf(tcp.source()));
				tcpHeader.put("destination port", String.valueOf(tcp.destination()));
				tcpHeader.put("sequence number", tcp.seq());
				tcpHeader.put("acknowledgement number", tcp.ack());
				tcpHeader.put("data offset", tcp.hlen());
				tcpHeader.put("window size", tcp.window());
				tcpHeader.put("ACK", tcp.flags_ACK());
				tcpHeader.put("CWR", tcp.flags_CWR());
				tcpHeader.put("ECE", tcp.flags_ECE());
				tcpHeader.put("URG", tcp.flags_URG());
				tcpHeader.put("PSH", tcp.flags_PSH());
				tcpHeader.put("RST", tcp.flags_RST());
				tcpHeader.put("SYN", tcp.flags_SYN());
				tcpHeader.put("FIN", tcp.flags_FIN());
				tcpHeader.put("checksum", tcp.checksum());
				tcpHeader.put("urgent pointer", tcp.urgent());
				if(tcp.getOffset() > 5) {
					int offset = tcp.hlen();
					int words = offset - 5;
					byte[] bytes = new byte[words*4];
					String byteString = "";
					for(int i = 0; i < words*4; i++) {
						bytes[i] = tcp.getByte(20+i);
						byteString += bytes[i];
					}
					//byte[] bytes = tcp.getByteArray(160, words*32);
					tcpHeader.put("options", byteString);
					packetJson.put("tcpHeader", tcpHeader);
				}
				//packetJson.put("content", packet.toString());
				
			}
			array.put(packetJson);
		}
		pcap.close();
		return array;
	}

	public void loadPcap(String file) throws SAXException {
		StringBuilder sb = new StringBuilder();
		Pcap pcap = Pcap.openOffline(file, sb);
		if (pcap == null) {
			System.err.println("Error opening pcap file " + file + " with error: " + sb.toString());
			if(!streamCalled) System.err.println("Copy of stream was not created.");
			return;
		}
		PcapPacketHandler<Queue<PcapPacket>> handler = new PcapPacketHandler<Queue<PcapPacket>>() {
			@Override
			public void nextPacket(PcapPacket packet, Queue<PcapPacket> queue) {
				PcapPacket permanent = new PcapPacket(packet);
				queue.offer(packet);
			}
		};
		Queue<PcapPacket> queue = new ArrayBlockingQueue<PcapPacket>(1000);
		pcap.loop(Pcap.NEXT_EX_EOF, handler, queue);
		Tcp tcp = new Tcp();
		Ip4 ip = new Ip4();
		int i = 0;
		while(!queue.isEmpty()) {
			PcapPacket packet = queue.poll();
			fXHTML.startElement("packet");
			if(packet.hasHeader(tcp)) {
				fXHTML.element("source port", String.valueOf(tcp.destination()));
				fXHTML.newline();
				fXHTML.element("destination port", String.valueOf(tcp.destination()));
				fXHTML.newline();
			}
			if(packet.hasHeader(ip)) {
				fXHTML.element("source address", bytesToIPString(ip.source()));
				fXHTML.newline();
				fXHTML.element("destination address", bytesToIPString(ip.destination()));
				
				fXHTML.newline();			
			}
			//handler.nextPacket(packet, queue);
			i++;
			fXHTML.endElement("packet");
		}
		System.out.println("i is " + i);
		pcap.close();
	}*/

	public String bytesToIPString(byte[] bytes) {
		String byteString = "";
		for(int i = 0; i < bytes.length; i++) {
			byteString += Byte.toUnsignedInt(bytes[i]) + ".";
		}
		return byteString;
	}

	public class InputStreamString {

	}
	

}
