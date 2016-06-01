package com.deleidos.dp.histogram;

import java.math.BigInteger;

public class ByteBucket extends AbstractBucket {
	byte byteLabel;
	
	public ByteBucket() {
		
	}
	
	public ByteBucket(byte label, BigInteger count) {
		super(String.valueOf(label), count);
		byteLabel = label;
	}

	public ByteBucket(byte byteLabel) {
		super();
		this.byteLabel = byteLabel; 
	}

	@Override
	public int belongs(Object object) {
		byte c = (byte) object;
		if(c > byteLabel) {
			return 1;
		} else if(c < byteLabel) {
			return -1;
		} else {
			return 0;
		}
	}

	@Override
	public String getLabel() {
		return String.valueOf(byteLabel);
	}

	@Override
	public int compareTo(AbstractBucket o) {
		ByteBucket otherBucket = (ByteBucket)o;
		int thisLabel = Byte.valueOf(this.getLabel());
		int otherLabel = Byte.valueOf(otherBucket.getLabel());
		if(thisLabel > otherLabel) {
			return 1;
		} else if(otherLabel > thisLabel) {
			return -1;
		} else {
			return 0;
		}
	}

}
