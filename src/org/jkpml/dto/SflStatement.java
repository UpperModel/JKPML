package org.jkpml.dto;

import java.util.LinkedList;
import java.util.List;

/**
 * A statement (a formal feature).
 * 
 * @author Daniel Couto-Vale <danielvale@icloud.com>
 */
public class SflStatement {

	/**
	 * The operator
	 */
	public final String operator;

	/**
	 * The arguments
	 */
	public final List<String> arguments;

	/**
	 * Constructor
	 * 
	 * @param operator the operator
	 */
	public SflStatement(String operator) {
		this.operator = operator;
		this.arguments = new LinkedList<String>();
	}

}
