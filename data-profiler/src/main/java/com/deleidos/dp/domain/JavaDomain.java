package com.deleidos.dp.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.interpretation.AbstractJavaInterpretation;
import com.deleidos.dp.interpretation.JavaInterpretation;
import com.deleidos.dp.interpretation.JavaUnknownInterpretation;

/**
 * Abstract classes for a "domain" in the schema wizard.  Subclasses should contain all possible Interpretations.  Subclasses of this mapping
 * should added to the <i>domainList</i> (see method <i>staticInit()</i>).
 * @author leegc
 *
 */
public abstract class JavaDomain {
	private static Logger logger = Logger.getLogger(JavaDomain.class);
	protected Map<String, AbstractJavaInterpretation> interpretationsMapping;

	public JavaDomain() {
		interpretationsMapping = new HashMap<String, AbstractJavaInterpretation>();
		addInterpretation(new JavaUnknownInterpretation());
	}

	/**
	 * Add an interpretation to the domain.  Should be used in a subclass' constructor.
	 * @param interpretation The interpretation to be added to the domain.
	 */
	public void addInterpretation(AbstractJavaInterpretation interpretation) {
		interpretationsMapping.put(interpretation.getInterpretationName().toLowerCase(), interpretation);
	}

	/**
	 * Get the domain name.
	 * @return domain name
	 */
	public abstract String getDomainName();

	/**
	 * Get the map of interpretations.  Key is interpretation name, value is the instance of the interpretation.
	 * @return
	 */
	public abstract Map<String, AbstractJavaInterpretation> getInterpretationMap();

	private static Map<String, Class<? extends JavaDomain>> domainList = null;

	/**
	 * Get the list of domains.  Key is domain name, value is the class (so it can be instantiated).
	 * @return all domains available
	 */
	public static Map<String, Class<? extends JavaDomain>> getDomainList() {
		if(domainList == null) {
			staticInit();
		}
		return domainList;
	}

	/**
	 * Initialize the domain list.  Add subclasses to this mapping.
	 */
	private static void staticInit() {
		domainList = new HashMap<String, Class<? extends JavaDomain>>();
		TransportationDomain t = new TransportationDomain();
		domainList.put(t.getDomainName().toLowerCase(), t.getClass());
	}

	/**
	 * Get a new instance of a domain based on its name.
	 * @param domainName the name of the domain
	 * @return a new instance of the domain
	 */
	public static JavaDomain getDomainByName(String domainName) {
		if(domainList == null) {
			staticInit();
		}
		try {
			return domainList.get(domainName.toLowerCase()).newInstance();
		} catch (InstantiationException e) {
			logger.error(e);
			Arrays.asList(e.getStackTrace()).forEach(x->logger.error(x));
		} catch (IllegalAccessException e) {
			logger.error(e);
			Arrays.asList(e.getStackTrace()).forEach(x->logger.error(x));
		}
		return null;
	}
}
