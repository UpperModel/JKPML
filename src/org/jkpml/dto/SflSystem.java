package org.jkpml.dto;

import java.util.LinkedList;
import java.util.List;

/**
 * A system.
 * 
 * @author Daniel Couto-Vale <danielvale@icloud.com>
 */
public class SflSystem implements SflNetworkNode {

	public final String name;
	public String region;
	public String chooser;
	public String metafunction;
	public List<SflFeature> features;
	public SflEntryCondition entryCondition;

	/**
	 * Constructor
	 * 
	 * @param name
	 */
	public SflSystem(String name) {
		this.name = name;
		this.features = new LinkedList<SflFeature>();
		this.metafunction = "Ideational".intern();
	}

}
