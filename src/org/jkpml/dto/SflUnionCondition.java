package org.jkpml.dto;

import java.util.LinkedList;
import java.util.List;

/**
 * A union condition.
 * 
 * @author Daniel Couto-Vale <danielvale@icloud.com>
 */
public class SflUnionCondition implements SflEntryCondition {
	
	public final List<SflEntryCondition> conditions;

	public SflUnionCondition() {
		this.conditions = new LinkedList<SflEntryCondition>();
	}

}
