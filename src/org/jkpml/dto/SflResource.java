package org.jkpml.dto;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A linguistic resource.
 * 
 * @author Daniel Couto-Vale <danielvale@icloud.com>
 */
public class SflResource {

	/**
	 * A system map.
	 */
	private final Map<String, SflSystem> systemMap;

	/**
	 * A gate map.
	 */
	private final Map<String, SflGate> gateMap;

	/**
	 * A feature map.
	 */
	private final Map<String, SflFeature> featureMap;

	private SflFeature rootFeature;

	/**
	 * Constructor
	 */
	public SflResource() {
		systemMap = new HashMap<String, SflSystem>();
		gateMap = new HashMap<String, SflGate>();
		featureMap = new HashMap<String, SflFeature>();
	}

	/**
	 * Makes a feature.
	 * 
	 * @param name the feature name
	 * @return the feature
	 */
	public final SflFeature makeFeature(String name) {
		SflFeature feature = featureMap.get(name.intern());
		if (feature == null) {
			feature = new SflFeature(name.intern());
			featureMap.put(feature.name, feature);
		}
		return feature;
	}

	public final SflFeature getFeature(String name) {
		return featureMap.get(name);
	}

	/**
	 * Makes a system.
	 * 
	 * @param name the system name
	 * @return the system
	 */
	public final SflSystem makeSystem(String name) {
		SflSystem system = systemMap.get(name);
		if (system == null) {
			system = new SflSystem(name.intern());
			systemMap.put(system.name, system);
		}
		return system;
	}

	/**
	 * Gets a system.
	 * 
	 * @param name the system name
	 * @return the system
	 */
	public final SflSystem getSystem(String name) {
		return systemMap.get(name);
	}

	/**
	 * Makes a gate.
	 * 
	 * @param name the gate name
	 * @return the gate
	 */
	public final SflGate makeGate(String name) {
		SflGate gate = gateMap.get(name);
		if (gate == null) {
			gate = new SflGate(name.intern());
			gateMap.put(gate.name, gate);
		}
		return gate;
	}

	/**
	 * Gets a gate.
	 * 
	 * @param name the gate name
	 * @return the gate
	 */
	public final SflGate getGate(String name) {
		return gateMap.get(name);
	}

	public final void setRootFeature(SflFeature feature) {
		rootFeature = feature;
	}

	public final SflFeature getRootFeature() {
		return rootFeature;
	}

	/**
	 * Gets a system map by region.
	 * 
	 * @return a system map
	 */
	public Map<String, List<SflNetworkNode>> getSystemMapByRegion() {
		Map<String, List<SflNetworkNode>> map = new HashMap<String, List<SflNetworkNode>>();
		for (SflSystem system : systemMap.values()) {
			List<SflNetworkNode> nodes = map.get(system.region);
			if (nodes == null) {
				nodes = new LinkedList<SflNetworkNode>();
				map.put(system.region, nodes);
			}
			nodes.add(system);
		}
		for (SflGate gate : gateMap.values()) {
			List<SflNetworkNode> nodes = map.get(gate.region);
			if (nodes == null) {
				nodes = new LinkedList<SflNetworkNode>();
				map.put(gate.region, nodes);
			}
			nodes.add(gate);
		}
		return map;
	}

}
