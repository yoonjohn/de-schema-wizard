package com.deleidos.dp.profiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.deleidos.dp.beans.Profile;

public class DisplayNameHelper {
	private static final Logger logger = Logger.getLogger(DisplayNameHelper.class);
	private static final String splitter = DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER;
	private static final long serialVersionUID = -6305121028412119474L;
	private int pathDepth;
	private String original;
	private String proposed;

	public DisplayNameHelper(String original) {
		this.pathDepth = countChar(splitter, original);
		this.original = original;
		this.proposed = dropPathPrefix(original, pathDepth);
	}

	public String getOriginal() {
		return original;
	}

	public void setOriginal(String original) {
		this.original = original;
	}

	public String getProposed() {
		return proposed;
	}
	
	public int getPathDepth() {
		return pathDepth;
	}

	public void setPathDepth(int pathDepth) {
		this.pathDepth = pathDepth;
	}

	public void stepUpHeirarchy() {
		this.proposed = dropPathPrefix(original, --pathDepth);
	}

	private String dropPathPrefix(String originalName, int numPathQualifiersToDrop) {
		String tmpName = originalName;
		for(int i = 0; i < numPathQualifiersToDrop; i++) {
			int index = tmpName.indexOf(splitter);
			if(index < 0) {
				return tmpName;
			} else {
				tmpName = tmpName.substring(index+1);
			}
		}
		return tmpName;
	}

	private static int countChar(String sep, String s) {
		return StringUtils.countMatches(s,  sep);
	}

	private static Map<String, List<DisplayNameHelper>> updateDisplayNameMap(Map<String, List<DisplayNameHelper>> original) {
		Map<String, List<DisplayNameHelper>> additions = new HashMap<String, List<DisplayNameHelper>>();
		List<String> removeables = new ArrayList<String>();
		for(String key : original.keySet()) {
			List<DisplayNameHelper> helperList = original.get(key);
			Iterator<DisplayNameHelper> helperListIterator = helperList.iterator();
			while(helperListIterator.hasNext()) {
				DisplayNameHelper helper = helperListIterator.next();
				if(!key.equals(helper.getProposed())) {
					if(original.containsKey(helper.getProposed())) {
						original.get(helper.getProposed()).add(helper);
					} else if(additions.containsKey(helper.getProposed())) {
						additions.get(helper.getProposed()).add(helper);
					} else {
						List<DisplayNameHelper> newHelperList = new ArrayList<DisplayNameHelper>();
						newHelperList.add(helper);
						additions.put(helper.getProposed(), newHelperList);
					}
					helperListIterator.remove();
				}
			}
			if(helperList.isEmpty()) {
				removeables.add(key);
			}
		}
		removeables.forEach(x->original.remove(x));
		original.putAll(additions);
		return original;
	}

	private static Map<String, List<DisplayNameHelper>> profileToDisplayNameMap(Map<String, Profile> profile) {
		Map<String, List<DisplayNameHelper>> displayNameMap = new HashMap<String, List<DisplayNameHelper>>();
		for(String key: profile.keySet()) {
			DisplayNameHelper helper = new DisplayNameHelper(key);
			if(displayNameMap.containsKey(helper.getProposed())) {
				displayNameMap.get(helper.getProposed()).add(helper);
			} else {
				List<DisplayNameHelper> helperList = new ArrayList<DisplayNameHelper>();
				helperList.add(helper);
				displayNameMap.put(helper.getProposed(), helperList);
			}
		}
		return displayNameMap;
	}
	
	private static boolean containsDuplicates(Map<String, List<DisplayNameHelper>> proposedDisplayNames) {
		for(String key : proposedDisplayNames.keySet()) {
			if(proposedDisplayNames.get(key).size() > 1) {
				return true;
			}
		}
		return false;
	}
	
	private static Map<String, List<DisplayNameHelper>> stepUpDuplicatePaths(Map<String, List<DisplayNameHelper>> proposedDisplayNames) {
		for(String key : proposedDisplayNames.keySet()) {
			if(proposedDisplayNames.get(key).size() > 1) {
				for(DisplayNameHelper helper : proposedDisplayNames.get(key)) {
					helper.stepUpHeirarchy();
				}
			}
		}
		return proposedDisplayNames;
	}
	
	private static Map<String, String> determineUniqueDisplayNames(Map<String, List<DisplayNameHelper>> nonUniqueDisplayNameMap) {
		// map proposedDisplayName -> list of helperClassesThat currently have that as a proposed name
		int maxIterations = 0;
		for(String proposed : nonUniqueDisplayNameMap.keySet()) {
			for(DisplayNameHelper helper : nonUniqueDisplayNameMap.get(proposed)) {
				maxIterations += helper.getPathDepth() + 1;
			}
		}
		for(int i = 0; i < maxIterations; i++) {
			if(containsDuplicates(nonUniqueDisplayNameMap)) {
				nonUniqueDisplayNameMap = updateDisplayNameMap(stepUpDuplicatePaths(nonUniqueDisplayNameMap));
			} else {
				break;
			}
		}
		Map<String, String> uniqueFieldToDisplayNameMapping = new HashMap<String, String>();
		for(String displayName : nonUniqueDisplayNameMap.keySet()) {
			if(nonUniqueDisplayNameMap.get(displayName).size() > 1) {
				nonUniqueDisplayNameMap.get(displayName).forEach(x->
					logger.error("Error determining unique key for " + x.getOriginal()));
			} else {
				uniqueFieldToDisplayNameMapping.put(
						nonUniqueDisplayNameMap.get(displayName).get(0).getOriginal(), displayName);
			}
		}
		return uniqueFieldToDisplayNameMapping;
	}

	public static Map<String, Profile> determineDisplayNames(Map<String, Profile> profile) {
		Map<String, List<DisplayNameHelper>> nonUniqueDisplayNameMap = profileToDisplayNameMap(profile);
		Map<String, String> displayNames = determineUniqueDisplayNames(nonUniqueDisplayNameMap);
		for(String key : displayNames.keySet()) {
			profile.get(key).setDisplayName(displayNames.get(key));
		}
		return profile;
	}

	/*public static Map<String, Profile> determineDisplayNames1(Map<String, Profile> profileMapping) {
		final String splitter = DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER;
		Map<String, String> displayNamesMapping = new HashMap<String, String>();
		//start with the deepest possible path node
		for(String s : profileMapping.keySet()) {
			displayNamesMapping.compute(s, (k,v)-> dropPathPrefix(s, splitter));
		}
		Set<String> keySet = displayNamesMapping.keySet();
		List<String> sortedKeys = new ArrayList<String>(keySet);
		sortedKeys.sort((x,y)->countChar(splitter, x) - countChar(splitter, y));
		for(String s : sortedKeys) {
			final int maxDepth = countChar(splitter, s);
			for(int i = 0; i <= maxDepth; i++) {
				List<String> duplicateNames = getDuplicateNames(s, displayNamesMapping);
				if(!duplicateNames.isEmpty()) {
					for(String duplicate : duplicateNames) {
						String newDisplayName = dropPathPrefix(duplicate, splitter, maxDepth-i);
						displayNamesMapping.put(duplicate, newDisplayName);
					}
				} else {
					break;
				}
			}
		}
		Map<String, List<String>> displayNames = new HashMap<String,List<String>>();
		for(String key : profileMapping.keySet()) {
			String displayName = displayNamesMapping.get(key);
			if(displayNames.containsKey(displayName)) {
				displayNames.get(displayName).add(key);
			} else {
				List<String> list = new ArrayList<String>();
				list.add(key);
				displayNames.put(displayName, list);
			}
		}
		for(String key : displayNames.keySet()) {

		}
		profileMapping.forEach((key, value) -> value.setDisplayName(displayNamesMapping.get(key)));
		return profileMapping;
	}*/
}
