package org.jkpml.dto;

import java.util.LinkedList;
import java.util.List;

/**
 * A intersection condition.
 * 
 * @author Daniel Couto-Vale <danielvale@icloud.com>
 */
public class SflIntersectionCondition implements SflEntryCondition {

	public final List<SflEntryCondition> conditions;

	public SflIntersectionCondition() {
		this.conditions = new LinkedList<SflEntryCondition>();
	}

}
