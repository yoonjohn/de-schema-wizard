package com.deleidos.dp.calculations;

import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.search.spell.JaroWinklerDistance;

import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.MatchingField;
import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.beans.StringDetail;
import com.deleidos.dp.enums.DetailType;
import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.profiler.DefaultProfilerRecord;
import com.deleidos.dp.profiler.DisplayNameHelper;
import com.deleidos.dp.profiler.api.ProfilingProgressUpdateHandler;

/**
 * Utility class to drive most of the "thinking" that needs to be done for metrics
 * @author leegc
 *
 */
public class MetricsCalculationsFacade {
	public static final MathContext DEFAULT_CONTEXT = MathContext.DECIMAL128;
	public static final MathContext SIMILARITY_CONTEXT = MathContext.DECIMAL32;
	private static final Logger logger = Logger.getLogger(MetricsCalculationsFacade.class);

	public static DetailType getDetailTypeFromDistribution(String mainType, int[] distribution) throws MainTypeException {
		return getDetailTypeFromDistribution(MainType.fromString(mainType), distribution);
	}

	public static DetailType getDetailTypeFromDistribution(MainType mainType, int[] distribution) throws MainTypeException {
		int m = -1;
		for(int i = 0; i < distribution.length; i++) {
			if(m > -1) {
				if(distribution[i] > distribution[m] && DetailType.getTypeByIndex(i).getMainType().equals(mainType)) {
					m = i;
				}
			} else {
				if(distribution[i] > 0 && DetailType.getTypeByIndex(i).getMainType().equals(mainType)) {
					m = i;
				}
			}
		}
		if(m > -1) {
			if(m == DetailType.INTEGER.getIndex() && distribution[DetailType.DECIMAL.getIndex()] > 0) {
				m = DetailType.DECIMAL.getIndex();
			}
			return DetailType.getTypeByIndex(m);
		} else {
			throw new MainTypeException(
					"Distribution could not be used to determine detail type for " + mainType.toString() + ".");
		}
	}

	public static MainType getDataTypeFromDistribution(int[] distribution, Tolerance errorLevel) {
		MainType type = null;
		int m = -1;
		for(int i = 0; i < distribution.length; i++) {
			if(distribution[i] > 0) {
				m = i;
			}
		}
		if(m > -1) {
			type = MainType.getTypeByIndex(m);
			if(type.equals(MainType.BINARY)) {
				return type;
			}
			if(distribution[MainType.NUMBER.getIndex()] == distribution[MainType.STRING.getIndex()]) {
				type = MainType.NUMBER;
			}
			if(type == MainType.NUMBER) {
				switch(errorLevel) {
				case STRICT: {
					if(distribution[MainType.STRING.getIndex()] > 0) {
						return MainType.STRING;
					}
				}
				case MODERATE: {
					float percentageString = (float)(distribution[MainType.STRING.getIndex()] 
							/ (float)(distribution[MainType.STRING.getIndex()] + distribution[MainType.BINARY.getIndex()] + distribution[MainType.NUMBER.getIndex()]));
					if(distribution[MainType.STRING.getIndex()] > 0 && percentageString > Tolerance.MODERATE.getAcceptableErrorsPercentage()) {
						return MainType.STRING;
					}
				}
				case RELAXED : {
					float percentageString = (float)(distribution[MainType.STRING.getIndex()] 
							/ (float)(distribution[MainType.STRING.getIndex()] + distribution[MainType.BINARY.getIndex()] + distribution[MainType.NUMBER.getIndex()]));
					if(distribution[MainType.STRING.getIndex()] > 0 && percentageString > Tolerance.RELAXED.getAcceptableErrorsPercentage()) {
						return MainType.STRING;
					}
				}
				}
			}
		}
		return type;
	}

	public static Number createNumberWithDoublePrecisionOrLower(Object value) throws MainTypeException {
		String stringValue = value.toString();
		if(NumberUtils.isNumber(stringValue)) {
			try {
				Double d = NumberUtils.createDouble(stringValue);
				if(d == null || d.isInfinite()) {
					throw new MainTypeException("Value " + stringValue + " is too precise for metrics accumulation.");
				} else if(d.longValue() == d.doubleValue()) {
					return d.longValue();
				} else {
					return d;
				}

			} catch (NumberFormatException e) {
				throw new MainTypeException("Value " + stringValue + " is too precise for metrics accumulation.");
			}
		} else {
			throw new MainTypeException("Value "+stringValue+" is non-numeric.");
		}
	}



	public static boolean isPossiblyNumeric(String stringValue) throws NumberFormatException {
		if(stringValue.isEmpty()) {
			return false;
		} else {
			return NumberUtils.isNumber(stringValue);
		}
	}

	public static float percentagePrintableCharacters(String stringValue) {
		int charCount = stringValue.length();
		if(charCount < 100) {
			return 1.0f;
		}
		int printableCharCount = 0;
		int nextInt = 0;

		for(int i = 0; i < charCount; i++) {
			nextInt = stringValue.charAt(i);
			if(isPrintableCharacter(nextInt)) {
				printableCharCount++;
			}
		}
		float printablePercentage = ((float)printableCharCount/(float)charCount);
		return printablePercentage;
	}

	public static boolean isPrintableCharacter(int c) {
		if(c > 32 && c < 127) return true;
		return false;
	}

	public static List<MainType> determineProbableDataTypes(Object value) {
		return determineProbableDataTypes(value, .3f);
	}

	/**
	 * Determine the data types that a value <i>probably</i> is.
	 * @param value The object being evaluated.
	 * @param binaryPercentageCutoff The percentage cutoff for when a string should be considered binary.
	 * @return a list of possible types, either binary or number and string.
	 */
	public static List<MainType> determineProbableDataTypes(Object value, float binaryPercentageCutoff) {
		if(value == null) {
			return Arrays.asList(MainType.NULL);
		}
		ArrayList<MainType> typeList = new ArrayList<MainType>();
		String stringValue = value.toString();
		if(value instanceof ByteBuffer) {
			typeList.add(MainType.BINARY);
			return typeList;
		}

		if(value instanceof Number) {
			typeList.add(MainType.NUMBER);
		} else if(value instanceof Boolean) {
			typeList.add(MainType.STRING);
		} else if(value instanceof String) {
			if(isPossiblyNumeric(stringValue)) {
				typeList.add(MainType.NUMBER);
			}
		} else if(value instanceof List) {
			typeList.add(MainType.ARRAY);
			return typeList;
		} else if(value instanceof Map) {
			typeList.add(MainType.OBJECT);
			return typeList;
		}
		typeList.add(MainType.STRING);
		return typeList;
	}

	/*public static MainType determineDataType(Object value) {
		MainType type = MainType.STRING;
		if(value.toString().isEmpty()) return MainType.STRING;
		if(value instanceof Number) {
			return MainType.NUMBER;
		}
		String stringValue = value.toString();
		boolean isAllNumbers = isPossiblyNumeric(stringValue);
		if(isAllNumbers) {
			return MainType.NUMBER;
		}
		if(stringValue.trim().startsWith("{") && stringValue.trim().endsWith("}")) {
			return MainType.OBJECT;
		} else if(value instanceof Map) {
			return MainType.OBJECT;
		}
		if(stringValue.trim().startsWith("[") && stringValue.trim().endsWith("]")) {
			return MainType.ARRAY;
		} else if(value instanceof AbstractList) {
			return MainType.ARRAY;
		}
		return type;
	}*/

	public static DetailType determineStringDetailType(Object object) {
		String stringValue = object.toString();
		String dateRegex = "(19|20)\\d\\d[- /.](0[1-9]|1[012])[- /.](0[1-9]|[12][0-9]|3[01])";
		Pattern datePattern = Pattern.compile(dateRegex);
		Matcher dateMatcher = datePattern.matcher(stringValue);
		if(dateMatcher.matches()) {
			return DetailType.DATE_TIME;
		} 
		if(stringValue.equals(DefaultProfilerRecord.EMPTY_FIELD_VALUE_INDICATOR)) {
			return DetailType.TERM;
		} else if(stringValue.contains(" ")) {
			// TODO Adjust if necessary
			if (stringValue.split(" ").length > 5) {
				return DetailType.TEXT;
			} else {
				return DetailType.PHRASE;
			}
		} else {
			String booleanRegex = "(TRUE|FALSE|true|false|T|F|t|f|yes|no|YES|NO|Yes|No|True|False)";
			Pattern bPattern = Pattern.compile(booleanRegex);
			Matcher bMatcher = bPattern.matcher(stringValue);
			if(bMatcher.matches()) return DetailType.BOOLEAN;
			return DetailType.TERM;
		}
	}

	public static DetailType determineNumberDetailType(Object object) {
		DetailType type = DetailType.DECIMAL;
		String stringValue = object.toString();
		if(stringValue.contains("e") || stringValue.contains("E") || stringValue.contains("^")) {
			type = DetailType.EXPONENT;
		} else if(stringValue.contains(".")) {
			type = DetailType.DECIMAL;
		} else {
			type = DetailType.INTEGER;
		} 
		return type;
	}

	public static DetailType determineBinaryDetailType(Object object) {
		DetailType type = null;
		return type;
	}

	/**
	 * Determine the detail type of the given record.
	 */
	public static DetailType determineDetailType(MainType mainType, Object object) {
		DetailType detailType = null;
		switch(mainType) {
		case STRING: {
			return MetricsCalculationsFacade.determineStringDetailType(object);
		}
		case NUMBER: {
			return MetricsCalculationsFacade.determineNumberDetailType(object);
		}
		case BINARY: {
			return MetricsCalculationsFacade.determineBinaryDetailType(object);
		}
		default:
			break;
		}
		return detailType;
	}

	public static double jaroWinklerComparison(String s1, String s2) {
		String lowerCaseS1 = s1.toLowerCase();
		String lowerCaseS2 = s2.toLowerCase();
		JaroWinklerDistance d = new JaroWinklerDistance();
		return d.getDistance(lowerCaseS1, lowerCaseS2);
	}

	private static double cosineSimilarity(Profile profile1, Profile profile2) {
		final int MIN = 0;
		final int MAX = 1;
		final int AVG = 2;
		final int STD = 3;
		final int DST = 4;
		final int VECTOR_SIZE = 5;
		if(profile1.getDetail().getDetailType().equals(profile2.getDetail().getDetailType()) 
				&& profile1.getMainType().equals(profile2.getMainType())) {

			String mainType = profile1.getMainType();
			// TODO need if above high cardinality threshold 
			double cosSim = 0;
			double[] v1 = new double[5];
			double[] v2 = new double[5];
			double numerator = 0;
			double denominator = 0;
			if(mainType.equals(MainType.NUMBER.toString())) {
				NumberDetail nm1 = (NumberDetail)profile1.getDetail();
				NumberDetail nm2 = (NumberDetail)profile2.getDetail();
				v1[MIN] = nm1.getMin().doubleValue();
				v2[MIN] = nm2.getMin().doubleValue();
				v1[MAX] = nm1.getMax().doubleValue();
				v2[MAX] = nm2.getMax().doubleValue();
				v1[AVG] = nm1.getAverage().doubleValue();
				v2[AVG] = nm2.getAverage().doubleValue();
				v1[STD] = nm1.getStdDev();
				v2[STD] = nm2.getStdDev();
				v1[DST] = stripNumDistinctValuesChars((nm1.getNumDistinctValues()));
				v2[DST] = stripNumDistinctValuesChars(nm2.getNumDistinctValues());
			} else if(mainType.equals(MainType.STRING.toString())) {
				StringDetail sm1 = (StringDetail)profile1.getDetail();
				StringDetail sm2 = (StringDetail)profile2.getDetail();
				v1[MIN] = sm1.getMinLength();
				v2[MIN] = sm2.getMinLength();
				v1[MAX] = sm1.getMaxLength();
				v2[MAX] = sm2.getMaxLength();
				v1[AVG] = sm1.getAverageLength();
				v2[AVG] = sm2.getAverageLength();
				v1[STD] = sm1.getStdDevLength();
				v2[STD] = sm2.getStdDevLength();
				v1[DST] = stripNumDistinctValuesChars(sm1.getNumDistinctValues());
				v2[DST] = stripNumDistinctValuesChars(sm2.getNumDistinctValues());
			} else {
				logger.error("Got binary for matching.");
				return 0.0;
			}
			for(int i = 0; i < VECTOR_SIZE; i++) {
				numerator += (v1[i] * v2[i]);
			}
			double sumOfSquaresA = 0;
			double sumOfSquaresB = 0;
			for(int i = 0; i < VECTOR_SIZE; i++) {
				sumOfSquaresA += Math.pow(v1[i], 2);
				sumOfSquaresB += Math.pow(v2[i], 2);
			}
			denominator = Math.sqrt(sumOfSquaresA) * Math.sqrt(sumOfSquaresB);
			cosSim = numerator/denominator;
			return cosSim;
		} else {
			return 0.0;
		}
	}

	private static double similarityAlgorithm1(String name1, Profile profile1, String name2, Profile profile2, double nameWeight) {
		final double statisticsWeight = 1 - nameWeight;
		double nameSimilarity = jaroWinklerComparison(name1, name2);
		double cosineSimilarity = cosineSimilarity(profile1, profile2);
		return (nameWeight * nameSimilarity) + (statisticsWeight * cosineSimilarity);
	}

	private static double similarityAlgorithm2(String name1, Profile profile1, String name2, Profile profile2,
			double disimilarityRate, double similarityArc, double nameWeight) {
		final double statisticsWeight = 1 - nameWeight;
		double nameSimilarity = jaroWinklerComparison(name1, name2);
		double newSimilarity = newSimilarity(name1, profile1, name2, profile2, disimilarityRate, similarityArc);
		return (nameWeight * nameSimilarity) + (statisticsWeight * newSimilarity);
	}

	private static double newSimilarity(String name1, Profile profile1, String name2, Profile profile2,
			double disimilarityRate, double similarityArc) {
		double newSimilarityCalc = 0;
		final int MIN = 0;
		final int MAX = 1;
		final int AVG = 2;
		final int STD = 3;
		final int DST = 4;
		if (!profile1.getMainType().equals(profile2.getMainType()) 
				|| !profile1.getDetail().getDetailType().equals(profile2.getDetail().getDetailType())) {
			return 0.0;
		} else {
			MainType mainType = MainType.fromString(profile1.getMainType());
			Vector<BigDecimal> v1 = new Vector<BigDecimal>();
			Vector<BigDecimal> v2 = new Vector<BigDecimal>();
			for(int i = 0; i < 5; i++) {
				v1.add(new BigDecimal(0, DEFAULT_CONTEXT));
				v2.add(new BigDecimal(0, DEFAULT_CONTEXT));
			}
			switch(mainType) {
			case NUMBER: {
				NumberDetail numberDetail1 = Profile.getNumberDetail(profile1);
				NumberDetail numberDetail2 = Profile.getNumberDetail(profile2);
				v1.set(MIN, numberDetail1.getMin());
				v1.set(MAX, numberDetail1.getMax());
				v1.set(AVG, numberDetail1.getAverage());
				v1.set(STD, BigDecimal.valueOf(numberDetail1.getStdDev()));
				v1.set(DST, new BigDecimal(stripNumDistinctValuesChars(numberDetail1.getNumDistinctValues())));

				v2.set(MIN, numberDetail2.getMin());
				v2.set(MAX, numberDetail2.getMax());
				v2.set(AVG, numberDetail2.getAverage());
				v2.set(STD, BigDecimal.valueOf(numberDetail2.getStdDev()));
				v2.set(DST, new BigDecimal(stripNumDistinctValuesChars(numberDetail2.getNumDistinctValues())));
				break;
			}
			case STRING: {
				StringDetail stringDetail1 = Profile.getStringDetail(profile1);
				StringDetail stringDetail2 = Profile.getStringDetail(profile2);

				v1.set(MIN, BigDecimal.valueOf(stringDetail1.getMinLength()));
				v1.set(MAX, BigDecimal.valueOf(stringDetail1.getMaxLength()));
				v1.set(AVG, BigDecimal.valueOf(stringDetail1.getAverageLength()));
				v1.set(STD, BigDecimal.valueOf(stringDetail1.getStdDevLength()));
				v1.set(DST, new BigDecimal(stripNumDistinctValuesChars(stringDetail1.getNumDistinctValues())));

				v2.set(MIN, BigDecimal.valueOf(stringDetail2.getMinLength()));
				v2.set(MAX, BigDecimal.valueOf(stringDetail2.getMaxLength()));
				v2.set(AVG, BigDecimal.valueOf(stringDetail2.getAverageLength()));
				v2.set(STD, BigDecimal.valueOf(stringDetail2.getStdDevLength()));
				v2.set(DST, new BigDecimal(stripNumDistinctValuesChars(stringDetail2.getNumDistinctValues())));

				break;
			}
			case BINARY: {
				logger.error("Got binary for similarity calculation.  Defaulting to zero.");
				return 0;
			}
			default: {
				logger.error("Not found as number, string, or binary.");
				return 0;
			}
			}

			BigDecimal normalizedMax = v1.get(MIN);
			BigDecimal normalizedMin = v1.get(MIN);

			for(BigDecimal value : v1) {
				normalizedMax = value.compareTo(normalizedMax) > 0 ? value : normalizedMax;
				normalizedMin = value.compareTo(normalizedMin) < 0 ? value : normalizedMin;
			}

			for(BigDecimal value : v2) {
				normalizedMax = value.compareTo(normalizedMax) > 0 ? value : normalizedMax;
				normalizedMin = value.compareTo(normalizedMin) < 0 ? value : normalizedMin;
			}

			normalizedMax = normalizedMax.divide(BigDecimal.valueOf(disimilarityRate), SIMILARITY_CONTEXT);
			normalizedMin = normalizedMin.divide(BigDecimal.valueOf(disimilarityRate), SIMILARITY_CONTEXT);

			for(int i = 0; i < v1.size(); i++) {
				BigDecimal normalizingDenominator = normalizedMax.subtract(normalizedMin).add(BigDecimal.ONE);

				BigDecimal updatedElement1;
				updatedElement1 = v1.get(i).subtract(normalizedMin);
				updatedElement1 = updatedElement1.divide(normalizingDenominator, SIMILARITY_CONTEXT);
				v1.set(i, updatedElement1);

				BigDecimal updatedElement2;
				updatedElement2 = v2.get(i).subtract(normalizedMin);
				updatedElement2 = updatedElement2.divide(normalizingDenominator, SIMILARITY_CONTEXT);
				v2.set(i, updatedElement2);

			}

			BigDecimal errorValueDenominator = BigDecimal.valueOf(v1.size());
			BigDecimal errorValueNumerator = BigDecimal.ZERO;
			for(int i = 0; i < v1.size(); i++) {
				// for readability
				//BigDecimal sub = v1.get(i).subtract(v2.get(i));
				//BigDecimal abs = sub.abs();
				//BigDecimal pow = abs.pow((int)similarityArc);
				errorValueNumerator = errorValueNumerator.add((v1.get(i).subtract(v2.get(i))).abs().pow((int)similarityArc));
			}
			BigDecimal error = errorValueNumerator.divide(errorValueDenominator);
			newSimilarityCalc = 1 / (1 + error.doubleValue());
			return newSimilarityCalc;
		}

	}

	public static Integer stripNumDistinctValuesChars(String numDistinctValues) {
		try {
			if(numDistinctValues.startsWith(">=")) {
				return Double.valueOf(numDistinctValues.substring(2)).intValue();
			} else {
				return Double.valueOf(numDistinctValues).intValue();
			}
		} catch (NumberFormatException e) {
			logger.error(e);
			logger.error("Could not get number of distinct values.");
			return 0;
		}
	}

	public static List<DataSample> matchFieldsAcrossSamplesAndSchema(Schema schema, List<DataSample> samples, 
			ProfilingProgressUpdateHandler progressCallback) {
		return matchFieldsAcrossSamplesAndSchema(schema, samples, new HashSet<String>(), progressCallback);
	}

	public static List<DataSample> matchFieldsAcrossSamplesAndSchema(Schema schema, List<DataSample> samples, 
			Set<String> ignoreGuids, ProfilingProgressUpdateHandler progressCallback) {
		if(progressCallback == null) {
			progressCallback = new ProfilingProgressUpdateHandler() {
				@Override
				public void handleProgressUpdate(long progress) {
					return;
				}
			};
		}
		int progress = 0;
		List<String> usedFieldNames = new ArrayList<String>();

		if(schema != null) {
			for(String schemaKey : schema.getsProfile().keySet()) {
				usedFieldNames.add(schemaKey);
				for(DataSample otherSample : samples) {
					if(!ignoreGuids.contains(otherSample.getDsGuid())) {
						for(String otherKey : otherSample.getDsProfile().keySet()) {
							String p1Name = schemaKey;
							Profile p1 = schema.getsProfile().get(p1Name);
							String p2Name = otherKey;
							Profile p2 = otherSample.getDsProfile().get(otherKey);

							double similarity = match(p1Name, p1, p2Name, p2);
							if(similarity > .8) {
								logger.debug("Match detected between " + p1Name + " in " + otherSample.getDsFileName() + " and " + p2Name + " in schema " + schema.getsName() + " with " + similarity + " confidence.");
								MatchingField altName = new MatchingField();
								List<MatchingField> altNames = p2.getMatchingFields();
								altName.setMatchingField(p1Name);
								altName.setConfidence((int)(similarity*100));
								altNames.add(altName);
								altNames.sort((MatchingField a1, MatchingField a2)->a2.getConfidence()-a1.getConfidence());
								p2.setMatchingFields(altNames);
							}
						}
					}
					progress++;
					progressCallback.handleProgressUpdate(progress);
				}
			}
		}

		for(DataSample sample : samples) {
			// dont analyze the error sample guids
			if(!ignoreGuids.contains(sample.getDsGuid())) {
				for(String key : sample.getDsProfile().keySet()) {
					if(!usedFieldNames.contains(key)) {
						sample.getDsProfile().get(key).setUsedInSchema(true);
						usedFieldNames.add(key);
					} else {
						progress++;
						continue; // seed value is already defined, skip analysis of this key
					}
					for(DataSample otherSample : samples) {
						if(ignoreGuids.contains(otherSample.getDsGuid()) || sample.equals(otherSample)) {
							continue; // skip same sample
						} else {
							for(String otherKey : otherSample.getDsProfile().keySet()) {
								String p1Name = key;
								Profile p1 = sample.getDsProfile().get(p1Name);
								String p2Name = otherKey;
								Profile p2 = otherSample.getDsProfile().get(otherKey);

								double similarity = match(p1Name, p1, p2Name, p2);

								if(similarity > .8) {
									logger.debug("Match detected between " + p1Name + " in " + sample.getDsFileName() + " and " + p2Name + " in " + otherSample.getDsFileName() + " with " + similarity + " confidence.");
									MatchingField altName = new MatchingField();
									List<MatchingField> altNames = p2.getMatchingFields();
									altName.setMatchingField(p1Name);
									altName.setConfidence((int)(similarity*100));
									altNames.add(altName);
									altNames.sort((MatchingField a1, MatchingField a2)->a2.getConfidence()-a1.getConfidence());
									p2.setMatchingFields(altNames);
								}
								progress++;
								progressCallback.handleProgressUpdate(progress);
							}
						}
					}
				}
			}
		}
		return samples;
	}

	public static double match(String name1, Profile p1, String name2, Profile p2) {
		String algId = System.getenv("SCHWIZ_MATCHING_ALG");
		// temporarily allow environment to specify matching algorithm
		if(p1.getPresence() < 0.0f || p2.getPresence() < 0.0f) {
			return 0.0;
		}

		if(name1.equals(DefaultProfilerRecord.EMPTY_FIELD_NAME_INDICATOR) || name2.equals(DefaultProfilerRecord.EMPTY_FIELD_NAME_INDICATOR)) {
			if(!name1.equals(name2)) {
				return 0.0; 
			}
		}
		if(name1.contains(String.valueOf(DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER))) {
			name1 = name1.substring(name1.lastIndexOf(DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER)+1, name1.length());
		}
		if(name2.contains(String.valueOf(DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER))) {
			name2 = name2.substring(name2.lastIndexOf(DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER)+1, name2.length());
		}


		final double nameWeight = .20;
		if("1".equals(algId)) {
			return match(name1, p1, name2, p2, nameWeight);
		} else if("2".equals(algId)) {
			// default is algorithm 2
			final double defaultDisimilarityRate = 1.25;
			final double defaultSimilarityArc = 6.0;
			return match(name1, p1, name2, p2, defaultDisimilarityRate, defaultSimilarityArc, nameWeight);
		} else {
			final double defaultDisimilarityRate = 1.25;
			final double defaultSimilarityArc = 6.0;
			return match(name1, p1, name2, p2, defaultDisimilarityRate, defaultSimilarityArc, nameWeight);
		}
	}

	public static double match(String name1, Profile p1, String name2, Profile p2, double nameWeight) {
		return MetricsCalculationsFacade.similarityAlgorithm1(name1, p1, name2, p2, nameWeight);
	}

	public static double match(String name1, Profile p1, String name2, Profile p2, double disimilarityRate, double similarityArc, double nameWeight) {
		return MetricsCalculationsFacade.similarityAlgorithm2(name1, p1, name2, p2, disimilarityRate, similarityArc, nameWeight);
	}

}
