package com.deleidos.dp.histogram;

import java.math.BigInteger;

public class TermBucket extends AbstractBucket { 
	String lowerBound;
	String upperBound = null;

	public TermBucket(String lowerBound, String upperBound) {
		super();
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}
	
	public TermBucket(String label, BigInteger count) {
		super(label, count);
		String[] splits = label.split(",", 2);
		if(splits.length > 1) {
			lowerBound = splits[0].substring(1);
			upperBound = splits[1].substring(0, splits[1].length()-1);
		} else {
			lowerBound = splits[0];
		}
	}
	
	public TermBucket(String term) {
		super();
		this.lowerBound = term;
	}

	@Override
	public int belongs(Object object) {
		String stringValue = object.toString();
		if(upperBound == null) {
			return stringValue.compareTo(lowerBound);
		} else {
			int lb = stringValue.compareTo(lowerBound);
			if(lb == 0) {
				//inclusive lower bound
				return 0;
			} else if(lb > 0) {
				if(stringValue.compareTo(upperBound) < 0) {
					//exclusive higher bound
					return 0;
				} else {
					return 1;
				}
			} else {
				return -1;
			}
		}
	}

	@Override
	public String getLabel() {
		if(upperBound == null) {
			return lowerBound;
		} else if(lowerBound == upperBound){
			return lowerBound; 
		} else {
			return "["+ lowerBound + "," + upperBound + "]";
		}
	}

	@Override
	public int compareTo(AbstractBucket o) {
		TermBucket otherBucket = (TermBucket)o;
		return this.lowerBound.compareTo(otherBucket.lowerBound);
	}



}
