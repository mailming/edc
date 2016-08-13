/*
 * Copyright (C) 2016 Turn Inc. All Rights Reserved.
 * Proprietary and confidential.
 */

package com.turn.edc.storage.impl;

import com.turn.edc.client.KeyNotFoundException;
import com.turn.edc.storage.StorageConnector;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.codec.binary.Base64;
import redis.clients.jedis.Jedis;

/**
 * Storage connector to Redis using Jedis library
 * (https://github.com/xetorthio/jedis)
 *
 * @author tshiou
 */
public class JedisStorageConnector implements StorageConnector {

	private final Jedis jedis;

	public JedisStorageConnector(String host, String port, int timeout) {
		// TODO: Sanity check host and port
		this.jedis = new Jedis(host, Integer.parseInt(port), timeout);
	}

	@Override
	public void store(String key, byte[] value, int ttl, int timeout) throws IOException {
		try {
			jedis.setex(key, ttl, Base64.encodeBase64String(value));
		} catch (Exception e) {
			throw new IOException(e.getCause());
		}
	}

	@Override
	public byte[] get(String key, int timeout) throws KeyNotFoundException, TimeoutException, IOException {
		String res;
		try {
			res = jedis.get(key);
		} catch (Exception e) {
			throw new IOException(e.getCause());
		}
		if (res == null) {
			throw new KeyNotFoundException();
		}
		return Base64.decodeBase64(jedis.get(key));
	}

	@Override
	public void close() {
		this.jedis.close();
	}
}
