/*
 * Copyright (C) 2016 Turn Inc. All Rights Reserved.
 * Proprietary and confidential.
 */

package com.turn.edc.exception;

/**
 * Add class description
 *
 * @author tshiou
 */
public class KeyNotFoundException extends Exception {
	public KeyNotFoundException(String key) {
		super("Key not found in cache: " + key);
	}
}
