package com.deleidos.dp.deserializors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.StructuredNode;
import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.profiler.DefaultProfilerRecord;

public class ConversionUtility {
	public static final int LEAF_NODES_FIRST = 1;
	public static final int TREE_NODES_FIRST = -1;
	public static final int SORT_STRATEGY = LEAF_NODES_FIRST;
	
	/*public static Map<String, Profile> convertToFlattenedMap(List<StructuredNode> heirarchicalProfiles) {
		Map<String, Profile> flattendMap = new HashMap<String, Profile>();
		heirarchicalProfiles.forEach(profile->
			flattendMap.putAll(extractRootProfileToMap(profile, false)));
		return flattendMap;
	}

	private static Map<String, Profile> extractRootProfileToMap(StructuredNode profile, boolean includeObjectProfiles) {
		Map<String, Profile> profileMap = new HashMap<String, Profile>();
		final String rootPath = profile.getField();
		if(profile.getChildren().size() <= 0) {
			profileMap.put(profile.getField(), profile);
		} else {
			profile.getChildren().forEach(child->
				profileMap.putAll(recursivelyExtractProfilesToMap(
						child, rootPath, includeObjectProfiles))); 
			if(includeObjectProfiles) {
				profileMap.put(profile.getField(), profile);
			}
		}
		return profileMap;
	}

	private static Map<String, Profile> recursivelyExtractProfilesToMap(StructuredNode profile, String currentPath, boolean includeObjectProfiles) {
		Map<String, Profile> profileMap = new HashMap<String, Profile>();
		final String concatenatedPath = currentPath+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+profile.getField();
		if(profile.getChildren().size() <= 0) {
			profileMap.put(concatenatedPath, profile);
		} else {
			profile.getChildren().forEach(child->{
				profileMap.putAll(recursivelyExtractProfilesToMap(child, concatenatedPath, includeObjectProfiles));
			});
			profile.setChildren(Arrays.asList());
			if(includeObjectProfiles) {
				profileMap.put(concatenatedPath, profile);
			}
		}
		return profileMap;
	}*/

	private static Map<String, Profile> addObjectProfilesToMap(Map<String, Profile> flattenedMap) {
		Map<String, Profile> objects = new HashMap<String, Profile>();
		flattenedMap.keySet().forEach(key-> {
			String[] splits = key.split("\\"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER);
			for(int i = 0; i < splits.length; i++) {
				StringBuilder concatenatedKey = new StringBuilder();
				concatenatedKey.append(splits[0]);
				objects.put(concatenatedKey.toString(), Profile.objectProfile());
				for(int j = 1; j < i; j++){
					concatenatedKey.append(DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+splits[j]);
					objects.put(concatenatedKey.toString(), Profile.objectProfile());
				}
			}
		});
		objects.forEach((k,v)->flattenedMap.putIfAbsent(k, v));
		// no longer need to convert to structured
		// structure list is just a reference
		/*flattenedMap.keySet().forEach(key->{
			flattenedMap.compute(key, (k,v)->v.toStructuredProfile(key));
		});*/
		return flattenedMap;	
	}

	private static StructuredNode recursivelyAddProfilesToProfile(
			Map<String, Profile> flattenedMap, String currentKey, final Incrementor inc) {
		String[] splits = currentKey.split("\\"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER);
		String fieldName = splits[splits.length-1];
		final StructuredNode node = new StructuredNode(currentKey, fieldName, inc.get());
		List<String> childrenKeys = getChildrenKeys(flattenedMap, currentKey);
		if(!childrenKeys.isEmpty()) {
			for(String child : childrenKeys) {
				node.getChildren().add( 
						recursivelyAddProfilesToProfile(flattenedMap, child, inc));
			}
		}
		return node;
	}

	private static List<String> getRootKeys(Map<String, Profile> flattenedMap) {
		final List<String> children = new ArrayList<String>();
		flattenedMap.keySet().forEach(potentialChild -> {
			if(StringUtils.countMatches(potentialChild,  DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER) == 0) {
				children.add(potentialChild);
			}
		});
		return sortChildren(flattenedMap, children, SORT_STRATEGY);
	}

	private static List<String> getChildrenKeys(final Map<String, Profile> flattenedMap, String key) {
		final List<String> children = new ArrayList<String>();
		final int depth =  StringUtils.countMatches(key,  DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER);
		flattenedMap.keySet().forEach(potentialChild -> {
			if(StringUtils.countMatches(potentialChild,  
					DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER) == depth + 1 && 
					potentialChild.startsWith(key)) {
				children.add(potentialChild);
			}
		});
		return sortChildren(flattenedMap, children, SORT_STRATEGY);
	}
	
	private static List<String> sortChildren(Map<String, Profile> originalMap, List<String> children, int sortingStrategy) {
		children.sort((String c1, String c2)-> {
			if(originalMap.get(c1).getMainTypeClass().equals(MainType.OBJECT)) {
				return sortingStrategy;
			} else if(originalMap.get(c2).getMainTypeClass().equals(MainType.OBJECT)) {
				return -sortingStrategy;
			} 
			return 0;
		});
		return children;
	}

	public static List<StructuredNode> convertToHeirarchicalList(Map<String, Profile> flattenedProfiles) {
		final List<StructuredNode> heirarchicalList = new ArrayList<StructuredNode>();
		final Map<String, Profile> copyOfFlattenedProfiles = new HashMap<String, Profile>(flattenedProfiles); 
		copyOfFlattenedProfiles.putAll(addObjectProfilesToMap(copyOfFlattenedProfiles));
		final Incrementor inc = new Incrementor(1);
		final List<String> rootObjects = getRootKeys(copyOfFlattenedProfiles);
		rootObjects.forEach(rootObject-> 
			heirarchicalList.add(recursivelyAddProfilesToProfile(copyOfFlattenedProfiles, rootObject, inc)));
		return heirarchicalList;
	}
	
	private static class Incrementor {
		int i;
		public Incrementor(int i) {this.i = i;}
		protected int get() { return i++; };
	}
}
