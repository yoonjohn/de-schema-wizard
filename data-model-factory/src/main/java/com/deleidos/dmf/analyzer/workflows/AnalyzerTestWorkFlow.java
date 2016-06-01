package com.deleidos.dmf.analyzer.workflows;

import java.util.HashMap;
import java.util.Map;

/**
 * Interface to handle static workflow instantiation.  Do not call this class directly, use AbstractAnalyzerTestWorkflow.
 * @author leegc
 *
 */
public interface AnalyzerTestWorkFlow {
	public static Map<Class<? extends AbstractAnalyzerTestWorkflow>, AbstractAnalyzerTestWorkflow> workflows 
	= new HashMap<Class<? extends AbstractAnalyzerTestWorkflow>, AbstractAnalyzerTestWorkflow>();
	public static AbstractAnalyzerTestWorkflow addOrGetWorkflow(AbstractAnalyzerTestWorkflow workflow) {
		if(workflows.containsKey(workflow.getClass())) {
			return workflows.get(workflow.getClass());
		} else {
			workflows.put(workflow.getClass(), workflow);
			return workflow;
		}
	}
}
