package com.googlecode.objectify.test.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 */
@Data
@NoArgsConstructor
public class Name implements Serializable {
	private String firstName;
	private String lastName;

	public Name(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}
}
