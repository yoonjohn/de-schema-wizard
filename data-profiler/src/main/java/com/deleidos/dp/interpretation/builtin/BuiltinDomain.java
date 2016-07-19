package com.deleidos.dp.interpretation.builtin;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class BuiltinDomain {
	public static final String name = "Default";
	private static Logger logger = Logger.getLogger(BuiltinDomain.class);
	protected Map<String, AbstractBuiltinInterpretation> interpretationsMapping;

	public BuiltinDomain() {
		interpretationsMapping = new HashMap<String, AbstractBuiltinInterpretation>();
		addInterpretation(new BuiltinUnknownInterpretation());
		addInterpretation(new BuiltinLatitudeInterpretation());
		addInterpretation(new BuiltinLongitudeInterpretation());
	}

	private void addInterpretation(AbstractBuiltinInterpretation interpretation) {
		interpretationsMapping.put(interpretation.getInterpretationName().toLowerCase(), interpretation);
	}

	public String getDomainName() {
		return name;
	}

	public Map<String, AbstractBuiltinInterpretation> getInterpretationMap() {
		return interpretationsMapping;
	}

	public static double degreesMinutesSecondsToDecimal(double degrees, double minutes, double seconds) {
		double decimalMinutes = minutes/60;
		double decimalSeconds = seconds/3600;
		double decimalValue = degrees + decimalMinutes + decimalSeconds;
		return decimalValue;
	}

}
