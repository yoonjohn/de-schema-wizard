package com.deleidos.dp.histogram;

import java.math.BigInteger;

public class CharacterBucket extends AbstractBucket {
	char characterLabel;
	
	public CharacterBucket(char label, BigInteger count) {
		super(String.valueOf(label), count);
		characterLabel = label;
	}

	public CharacterBucket(String label, BigInteger count) {
		super(String.valueOf(label.charAt(0)), count);
		characterLabel = label.charAt(0);
	}

	public CharacterBucket(char characterLabel) {
		super();
		this.characterLabel = characterLabel; 
	}

	@Override
	public int belongs(Object object) {
		char c = (char) object;
		if(c > characterLabel) {
			return 1;
		} else if(c < characterLabel) {
			return -1;
		} else {
			return 0;
		}
	}

	@Override
	public String getLabel() {
		return String.valueOf(characterLabel);
	}

	@Override
	public int compareTo(AbstractBucket o) {
		CharacterBucket otherBucket = (CharacterBucket)o;
		char thisLabel = this.getLabel().charAt(0);
		char otherLabel = otherBucket.getLabel().charAt(0);
		if(thisLabel > otherLabel) {
			return 1;
		} else if(otherLabel > thisLabel) {
			return -1;
		} else {
			return 0;
		}
	}

}
