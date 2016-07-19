package com.deleidos.dmf.parser;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.jnetpcap.Pcap;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.network.Ip6;
import org.jnetpcap.protocol.tcpip.Tcp;
import org.jnetpcap.protocol.tcpip.Udp;
import org.xml.sax.ContentHandler;

import com.deleidos.dmf.exception.AnalyticsTikaProfilingException;
import com.deleidos.dmf.framework.AbstractAnalyticsParser;
import com.deleidos.dmf.framework.AnalyticsEmbeddedDocumentExtractor;
import com.deleidos.dmf.framework.TikaProfilerParameters;
import com.deleidos.dmf.parser.pcap.ext.Wireless80211;
import com.deleidos.dp.profiler.DefaultProfilerRecord;
import com.deleidos.dp.profiler.api.ProfilerRecord;

/**
 * Parse for pcap files
 * @author leegc
 *
 */
public class JNetPcapTikaParser extends AbstractAnalyticsParser {
	private static final Logger logger = Logger.getLogger(JNetPcapTikaParser.class);
	public static final MediaType CONTENT_TYPE = MediaType.application("vnd.tcpdump.pcap");
	private static final Set<MediaType> SUPPORTED_TYPES = 
			Collections.singleton(MediaType.application("vnd.tcpdump.pcap"));
	private static StringBuilder errorBuffer;
	private Pcap pcap;
	private PcapPacketCallbackHandler jPacketCallback;
	private int packetBufferSize = 100;
	private int packetsRead;
	private int snaplen;
	private boolean output = true;

	public JNetPcapTikaParser() {
		packetsRead = 0;
	}

	public static String macBytesToString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < bytes.length; i++) {
			String s = Integer.toHexString(Byte.toUnsignedInt(bytes[i]));
			if(s.length() == 1) {
				s = "0" + s;
			}
			sb.append(s + ":");
		}
		return sb.toString().substring(0, sb.toString().length()-1);
	}

	public static String macBytesToString(int[] bytes) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < bytes.length; i++) {
			String s = Integer.toHexString(bytes[i]);
			if(s.length() == 1) {
				s = "0" + s;
			}
			sb.append(s + ":");
		}
		return sb.toString().substring(0, sb.toString().length()-1);
	}

	public static String ipv4ToString(int ip) {
		return Integer.toString(ip >> 24) + "." + Integer.toString((ip >> 16) & 0xff)
		+ "." + Integer.toString((ip >> 8) & 0xff) + "." + Integer.toString(ip & 0xff);
	}

	public static String ipv4ToString(byte[] ip) {
		return Integer.toString(Byte.toUnsignedInt(ip[0])) + "." + Integer.toString(Byte.toUnsignedInt(ip[1]))
		+ "." + Integer.toString(Byte.toUnsignedInt(ip[2])) + "." + Integer.toString(Byte.toUnsignedInt(ip[3]));
	}

	public static String ipv6ToString(byte[] ip6) {
		StringBuilder sb = new StringBuilder();
		if(ip6.length == 8 || ip6.length == 16) {
			for(int i = 0; i < ip6.length/2; i++) {
				sb.append(Integer.toHexString(ip6[i]));
				sb.append(Integer.toHexString(ip6[i+1]));
				sb.append(":");
			}
			String s = sb.toString();
			return s.substring(0, s.length()-1);
		} else {
			return null;
		}
	}

	@Override
	public void preParse(InputStream inputStream, ContentHandler handler, Metadata metadata,
			TikaProfilerParameters context) throws AnalyticsTikaProfilingException {
		errorBuffer = new StringBuilder();
		String file = context.get(File.class).getAbsolutePath();
		String metadataFile = metadata.get(AnalyticsEmbeddedDocumentExtractor.RESOURCE_PATH_KEY);
		// metadata contains the key if the file was extracted
		// file will never be null, but metadataFile could be null
		if(metadataFile != null) {
			if(!file.equals(metadataFile)) {
				// use the metadata file because 
				file = metadataFile;
			}
		}
		logger.info("Opening " + file + " to parse pcap.");
		//System.out.println(JRegistry.toDebugString());
		pcap = Pcap.openOffline(file, errorBuffer);
		snaplen = pcap.snapshot();
		if(pcap.isSwapped() == 1) {
			logger.warn("Pcap file detected as swapped.");
		}
		jPacketCallback = new PcapPacketCallbackHandler();
	}

	@Override
	public Set<MediaType> getSupportedTypes(ParseContext context) {
		return SUPPORTED_TYPES;
	}

	@Override
	public ProfilerRecord getNextProfilerRecord(InputStream inputStream, ContentHandler handler, Metadata metadata,
			TikaProfilerParameters context) throws AnalyticsTikaProfilingException {
		// don't even use stream because JnetPcap must handle it internally
		if(jPacketCallback.getPacketBuffer().isEmpty()) {
			packetsRead += pcap.loop(packetBufferSize, jPacketCallback, null);
		} 

		if(jPacketCallback.getPacketBuffer().isEmpty()) {
			return null;
		} else {
			PcapPacket packet = jPacketCallback.getPacketBuffer().remove(0);
			int size = packet.getTotalSize();
			context.setCharsRead(context.getCharsRead()+size);
			DefaultProfilerRecord record = extractHeadersAndAddToRecord(packet);
			//record.setRecordProgress(context.getCharsRead()+size);
			return record;
		}
	}
	static int i=0;
	private DefaultProfilerRecord extractHeadersAndAddToRecord(PcapPacket packet) {
		DefaultProfilerRecord record = new DefaultProfilerRecord();

		/* possibly iterate over all headers and add fields to profile record using annotations?
		 */

		// get any/all fields from ethernet header
		Ethernet ethernet = new Ethernet();
		if(packet.hasHeader(ethernet)) {
			ethernet = packet.getHeader(ethernet);
			record.put("eth.type", ethernet.typeEnum());
			record.put("eth.dst", macBytesToString(ethernet.destination()));
			record.put("eth.src", macBytesToString(ethernet.source()));
		}

		/* 
		 * get any/all wireless header fields
		 * 
		 * because of the binding the JNetPcap API picks up on 
		 * the wireless header even if there is a radiotap preamble
		 * through the @Bind annotation
		 * */
	
		Wireless80211 wirelessHeader = new Wireless80211();
		String s = packet.toHexdump();
		if(packet.hasHeader(wirelessHeader)) {
			wirelessHeader = packet.getHeader(wirelessHeader);
			try {
				wirelessHeader.addSuccessfullyParsedValuesToMap(record);
			} catch (Exception e) {
				logger.error("Did not successfully parse wireless header.");
				logger.error(e);
				logger.error(packet.toHexdump());
			}
		} 

		// get IPv4 fields
		Ip4 ip4 = new Ip4();
		if(packet.hasHeader(ip4)) {
			ip4 = packet.getHeader(ip4);	
			record.put("ip.src", ipv4ToString(ip4.source()));
			record.put("ip.dest", ipv4ToString(ip4.destination()));
		}

		// get IPv6 fields
		Ip6 ip6 = new Ip6();
		if(packet.hasHeader(ip6)) {
			ip6 = packet.getHeader(ip6);
			record.put("ipv6.dst", ipv6ToString(ip6.destination()));
			record.put("ipv6.src", ipv6ToString(ip6.source()));
		}

		// get UDP fields
		Udp udp = new Udp();
		if(packet.hasHeader(udp)) {
			udp = packet.getHeader(udp);
			record.put("udp.dstport", udp.destination());
			record.put("udp.srcport", udp.source());
		}

		// get TCP fields
		Tcp tcp = new Tcp();
		if(packet.hasHeader(tcp)) {
			Tcp pTcp = packet.getHeader(tcp);
			record.put("tcp.srcport", pTcp.source());
			record.put("tcp.dstport", pTcp.destination());
		}
		return record;
	}

	public static byte[] swap(byte[] b) {
		byte[] tmp = new byte[b.length];
		for(int i = 0; i < b.length; i++) {
			tmp[i] = b[b.length-i-1]; 
		}
		return tmp;
	}

	@Override
	public void postParse(ContentHandler handler, Metadata metadata, TikaProfilerParameters context)
			throws AnalyticsTikaProfilingException {
		super.postParse(handler, metadata, context);
		pcap.close();
	}

	private class PcapPacketCallbackHandler implements PcapPacketHandler<Object> {
		private List<PcapPacket> packetBuffer;

		public PcapPacketCallbackHandler() {
			packetBuffer = new ArrayList<PcapPacket>();
		}

		@Override
		public void nextPacket(PcapPacket packet, Object user) {
			packetBuffer.add(packet);
		}

		public List<PcapPacket> getPacketBuffer() {
			return packetBuffer;
		}

		public void setPacketBuffer(List<PcapPacket> packetBuffer) {
			this.packetBuffer = packetBuffer;
		}
	}

}
