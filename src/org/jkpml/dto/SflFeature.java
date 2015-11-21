package org.jkpml.dto;

import java.util.LinkedList;
import java.util.List;

/**
 * A systemic feature.
 * 
 * @author Daniel Couto-Vale <danielvale@icloud.com>
 */
public class SflFeature {

	public String name;
	public List<SflStatement> statements;
	public double probability;
	public boolean active;
	public String gloss;
	public String comment;

	protected SflFeature(String name) {
		this.name = name;
		this.statements = new LinkedList<SflStatement>();
		this.gloss = "".intern();
		this.comment = "".intern();
	}

}
