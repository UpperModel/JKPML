package org.jkpml.dto;

/**
 * A feature condition.
 * 
 * @author Daniel Couto-Vale <danielvale@icloud.com>
 */
public class SflFeatureCondition implements SflEntryCondition {

	/**
	 * The feature.
	 */
	public final SflFeature feature;

	/**
	 * Constructor
	 * 
	 * @param feature the feture
	 */
	public SflFeatureCondition(SflFeature feature) {
		this.feature = feature;
	}

}
