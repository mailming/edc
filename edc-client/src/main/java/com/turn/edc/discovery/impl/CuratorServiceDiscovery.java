/*
 * Copyright (C) 2016 Turn Inc. All Rights Reserved.
 * Proprietary and confidential.
 */

package com.turn.edc.discovery.impl;

import com.turn.edc.discovery.CacheInstance;
import com.turn.edc.discovery.CuratorSerializer;
import com.turn.edc.discovery.DiscoveryListener;
import com.turn.edc.discovery.ServiceDiscovery;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Zookeeper-based service discovery connector
 *
 * Uses Curator API
 *
 * @author tshiou
 */
public class CuratorServiceDiscovery extends DiscoveryListener implements ServiceDiscovery {

	private static final Logger LOG = LoggerFactory.getLogger(CuratorServiceDiscovery.class);

	private final CuratorFramework curator;

	private final org.apache.curator.x.discovery.ServiceDiscovery serviceDiscovery;

	private final ServiceCache<CacheInstance> serviceCache;

	private List<CacheInstance> liveInstances;

	private List<DiscoveryListener> listeners = Lists.newArrayList();

	@SuppressWarnings("unchecked")
	public CuratorServiceDiscovery(String zkConnectionString, String serviceName) {
		this.curator = CuratorFrameworkFactory.newClient(zkConnectionString, new ExponentialBackoffRetry(1000, 3));

		this.serviceDiscovery = ServiceDiscoveryBuilder.builder(CacheInstance.class)
				.client(curator)
				.basePath("/edc")
				.serializer(new CuratorSerializer())
				.build();

		this.serviceCache = serviceDiscovery.serviceCacheBuilder()
				.name(serviceName)
				.build();

		attachListeners(this);
	}

	@Override
	public void start() throws IOException {

		try {
			if (curator.getState() != CuratorFrameworkState.STARTED) {
				curator.start();
			}
			this.serviceDiscovery.start();
			this.serviceCache.start();

			// Initialize instances
			initializeInstances(this.serviceCache);
			for (DiscoveryListener listener : this.listeners) {
				listener.update(this.liveInstances);
			}
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public void shutdown() {
		try {
			this.serviceCache.close();
		} catch (IOException e) {
			LOG.error("Failed to close service discovery cache: " + ExceptionUtils.getStackTrace(e));
		}

		try {
			this.serviceDiscovery.close();
		} catch (IOException e) {
			LOG.error("Failed to close service discovery: " + ExceptionUtils.getStackTrace(e));
		}
	}

	public List<CacheInstance> getAvailableInstances() {
		return this.liveInstances;
	}

	@Override
	public void attachListeners(DiscoveryListener... listeners) {
		for (DiscoveryListener listener : listeners) {
			this.serviceCache.addListener(new CuratorCacheListener(this.serviceCache, listener));
			this.listeners.add(listener);
		}
	}

	@Override
	public void update(List<CacheInstance> instances) {
		this.liveInstances = instances;
	}

	private void initializeInstances(ServiceCache<CacheInstance> initializedCache) {
		List<CacheInstance> newList = Lists.newArrayList();

		List<ServiceInstance<CacheInstance>> instances = initializedCache.getInstances();
		for (ServiceInstance<CacheInstance> instance : instances) {
			newList.add(instance.getPayload());
		}

		update(newList);
	}
}
