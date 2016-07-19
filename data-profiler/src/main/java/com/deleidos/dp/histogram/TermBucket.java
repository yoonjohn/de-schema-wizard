package com.deleidos.dp.histogram;

import java.math.BigInteger;

public class TermBucket extends AbstractBucket { 
	private static final int TEMP_DEFAULT_TERM_BUCKET_CUTOFF = 12;
	String lowerBound;
	String upperBound = null;

	public TermBucket(String lowerBound, String upperBound) {
		super();
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}
	
	public TermBucket(String label, BigInteger count) {
		super(count);
		String[] labels = parseLabels(label);
		this.lowerBound = labels[0];
		this.upperBound = labels[1];
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
	
	public static String[] parseLabels(String rawLabel) {
		String[] labels = new String[2];
		String[] splits = rawLabel.split(",", 2);
		if(splits.length > 1) {
			labels[0] = splits[0].substring(1);
			labels[1] = splits[1].substring(0, splits[1].length()-1);
		} else {
			labels[0] = splits[0];
		}
		return labels;
	}
	
	private static String trimLabel(String label, int cutoff) {
		if(label.length() > cutoff) {
			return label.substring(0, cutoff) + "...";
		}
		return label;
	}
	
	public static String trimRawLabel(String rawLabel, int cutoff) {
		String[] labels = parseLabels(rawLabel);
		if(labels[1] == null) {
			return trimLabel(labels[0], cutoff);
		} else if(labels[0] == labels[1]){
			return trimLabel(labels[0], cutoff); 
		} else {
			return "["+ trimLabel(labels[0], cutoff) + "," + trimLabel(labels[1], cutoff) + "]";
		}
	}
	
	private static String generateRawLabel(String lowerBound, String upperBound) {
		if(upperBound == null) {
			return lowerBound;
		} else if(lowerBound == upperBound){
			return lowerBound; 
		} else {
			return "["+ lowerBound + "," + upperBound + "]";
		}
	}

	@Override
	public String getLabel() {
		return generateRawLabel(lowerBound, upperBound);
	}

	@Override
	public int compareTo(AbstractBucket o) {
		TermBucket otherBucket = (TermBucket)o;
		return this.lowerBound.compareTo(otherBucket.lowerBound);
	}



}
