package org.jkpml.dto;

/**
 * A gate.
 * 
 * @author Daniel Couto-Vale <danielvale@icloud.com>
 */
public class SflGate implements SflNetworkNode {

	public final String name;
	public String region;
	public SflFeature feature;
	public SflEntryCondition entryCondition;

	/**
	 * Constructor
	 * 
	 * @param name
	 */
	public SflGate(String name) {
		this.name = name;
	}

}
