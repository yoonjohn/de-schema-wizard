package com.deleidos.dp.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.deleidos.dp.interpretation.AbstractJavaInterpretation;
import com.deleidos.dp.interpretation.JavaInterpretation;
import com.deleidos.dp.interpretation.JavaLatitudeInterpretation;
import com.deleidos.dp.interpretation.JavaLongitudeInterpretation;

/**
 * Domain for transportation data.  Contains longitude and latitude interpretations (12/17).
 * @author leegc
 *
 */
public class TransportationDomain extends JavaDomain {
	public static final String name = "transportation";
	/**
	 * 
	 */
	private static final long serialVersionUID = -3618889657033733995L;

	public TransportationDomain() {
		super();
		addInterpretation(new JavaLatitudeInterpretation());
		addInterpretation(new JavaLongitudeInterpretation());
	}

	@Override
	public String getDomainName() {
		return name;
	}

	@Override
	public Map<String, AbstractJavaInterpretation> getInterpretationMap() {
		return interpretationsMapping;
	}

	public static double degreesMinutesSecondsToDecimal(double degrees, double minutes, double seconds) {
		double decimalMinutes = minutes/60;
		double decimalSeconds = seconds/3600;
		double decimalValue = degrees + decimalMinutes + decimalSeconds;
		return decimalValue;
	}

}
