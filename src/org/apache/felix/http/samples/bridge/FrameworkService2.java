/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.samples.bridge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.felix.framework.Felix;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.launch.Framework;

/**
 * 
 * NOT USED IN ITS CURRENT FORM
 * @author frank
 *
 */
public final class FrameworkService2 {
	private final ServletContext context;
	private Framework felix;
	private final static String APPSERVERBUNDLEDIR = "bundles/";
	private final static String BUNDLEDIR = "WEB-INF/bundles/";
	private final static String EXPLICITBUNDLEDIR = "WEB-INF/explicit/";

	public FrameworkService2(ServletContext context) {
		this.context = context;
	}

	public static void main(String[] args) throws BundleException,
			MalformedURLException, InterruptedException {
		FrameworkService2 fs = new FrameworkService2(null);
		fs.start();
	}

	public Bundle install(String path) throws MalformedURLException {
		Bundle installedBundle;
		try {
			installedBundle = felix.getBundleContext().installBundle(
					getUrl(EXPLICITBUNDLEDIR + path).toString());
			installedBundle.start();
			return installedBundle;
		} catch (BundleException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void start() {
		try {
			doStart();
			install("org.apache.felix.configadmin_1.2.8.jar");
			install("org.apache.felix.fileinstall-3.2.0.jar");

		} catch (Exception e) {
			log("Failed to start framework", e);
		}
	}

	public void stop() {
		try {
			doStop();
		} catch (Exception e) {
			log("Error stopping framework", e);
		}
	}

	private void doStart() throws Exception {

		// HttpContext a;

		// FrameworkFactory frameworkFactory = ServiceLoader.load(
		// FrameworkFactory.class).iterator().next();
		// Map<String, String> config = new HashMap<String, String>();
		Map<String, Object> createConfig = createConfig();
		Framework framework = new Felix(createConfig);
		log(framework.getSymbolicName(), null);
		framework.init();
		framework.start();
		this.felix = framework;
		if (context != null) {
			log("Setting "+BundleContext.class.getName()+" : "+framework.getBundleContext(),null);
//			context.setAttribute(BundleContext.class.getName(),framework.getBundleContext());
		}

		log("OSGi framework started", null);
		framework.getBundleContext().addBundleListener(new BundleListener() {

			@Override
			public void bundleChanged(BundleEvent be) {
				// log("BundleChanged: "+be.getBundle().getSymbolicName()+" "+
				// be.getType(),null);
				// log("ID: "+be.getBundle().getBundleId(),null);
			}
		});
	}

	private void doStop() throws Exception {
		if (this.felix != null) {
			this.felix.stop();
		}

		log("OSGi framework stopped", null);
	}

	// TODO: add some config properties
	private Map<String, Object> createConfig() throws Exception {
		Properties props = new Properties();
		props.load(getResource("/WEB-INF/framework.properties"));

		Map<String, Object> map = new HashMap<String, Object>();
		for (Object key : props.keySet()) {
			String value = (String) props.get(key);
			log("key: " + key + " value: " + value, null);
			map.put(key.toString(), value);
		}
		StringBuffer bundlePath = new StringBuffer();
		String resolvedBundlePath = getFilePath(BUNDLEDIR).toString();
		bundlePath.append(resolvedBundlePath);
		if (context == null) {
			// We have no 'containing' appserver, so we build our own. Append
			// the appserver bundle path.
			String localBundlePath = getFilePath(APPSERVERBUNDLEDIR).toString();
			bundlePath.append(",");
			bundlePath.append(localBundlePath);
		}
		log(bundlePath.toString(), null);
		System.err.println("Resolved: " + bundlePath.toString());
		map.put("felix.fileinstall.dir", bundlePath.toString());
		map.put("felix.fileinstall.log.level", "5");
		map.put("felix.fileinstall.noInitialDelay", "true");

		map.put("SYSTEMBUNDLE_ACTIVATORS_PROP",
				Arrays.asList(new ProvisionActivator(this.context)));
		return map;
	}

	// felix.fileinstall.dir

	private URL getUrl(String path) throws MalformedURLException {
		return getFilePath(path).toURI().toURL();
	}

	private File getFilePath(String path) {
		if (this.context == null) {
			File f = new File(System.getProperty("user.dir"));
			File res = new File(f, path);
			// TODO: This stream should be closed, is that going well?
			return res;
		}
		File f = new File(this.context.getRealPath(path));
		return f;
	}

	private InputStream getResource(String path) throws FileNotFoundException {
		if (this.context == null) {
			File f = new File(System.getProperty("user.dir"));
			File res = new File(f, path);
			// TODO: This stream should be closed, is that going well?
			return new FileInputStream(res);
		}
		return this.context.getResourceAsStream(path);
	}

	private void log(String message, Throwable cause) {
		if (context == null) {
			System.err.println("NO CONTEXT: " + message);
			if (cause != null) {
				cause.printStackTrace();
			}
			return;
		}
		this.context.log(context.getContextPath() + ": " + message, cause);
		System.err.println("> " + message);
		if (cause != null) {
			cause.printStackTrace();
		}
	}
}
