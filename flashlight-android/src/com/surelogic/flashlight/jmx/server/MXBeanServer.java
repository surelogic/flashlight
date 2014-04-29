package com.surelogic.flashlight.jmx.server;

import java.io.*;
import java.lang.management.*;
import java.net.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;

import javax.management.*;
import javax.management.remote.*;
import javax.management.remote.rmi.RMIConnectorServer;

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.VMOption;

/* 
 * Derived from com.j256.simplejmx.server.JmxServer
 * 1. Removed the extras we didn't need
 * 2. Changed to use a fresh MBeanServer
 */
public class MXBeanServer {
	private final String RMI_SERVER_HOST_NAME_PROPERTY = "java.rmi.server.hostname";
	private static final String HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";
	private static final int PORT = 1100;
	
	public static void main(String[] args) throws Exception {
		MXBeanServer server = new MXBeanServer(PORT);
		server.start();
		try {
			server.setupMXBeans();
			Thread.sleep(1000000000);
		} finally {
			server.stop();
		}
	}
	
	// Stub that will eventually redirect to Flashlight
	void setupMXBeans() throws JMException {
		setupOtherMXBeans();
		
		register(ManagementFactory.RUNTIME_MXBEAN_NAME, new RuntimeMXBean() {			
			@Override
			public boolean isBootClassPathSupported() {
				return false;
			}
			
			@Override
			public String getVmVersion() {
				return "1.6";
			}
			
			@Override
			public String getVmVendor() {
				return "SureLogic";
			}
			
			@Override
			public String getVmName() {
				return "Flashlight";
			}
			
			@Override
			public long getUptime() {
				return 1000000000;
			}
			
			@Override
			public Map<String, String> getSystemProperties() {
				return Collections.emptyMap();
			}
			
			@Override
			public long getStartTime() {
				return 0;
			}
			
			@Override
			public String getSpecVersion() {
				return "1.5";
			}
			
			@Override
			public String getSpecVendor() {
				return "Google";
			}
			
			@Override
			public String getSpecName() {
				return "Android";
			}
			
			@Override
			public String getName() {
				return "Anonymous";
			}
			
			@Override
			public String getManagementSpecVersion() {
				return "1.0";
			}
			
			@Override
			public String getLibraryPath() {
				return "No_libraries";
			}
			
			@Override
			public List<String> getInputArguments() {
				return Collections.singletonList("JustThis");
			}
			
			@Override
			public String getClassPath() {
				return "Unknown_class_path";
			}
			
			@Override
			public String getBootClassPath() {
				return "Unknown_boot_path";
			}
		});
		// TODO customize
		register(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, new SunOSMXBean() {			
			@Override
			public String getVersion() {
				return "5.0";
			}
			
			@Override
			public double getSystemLoadAverage() {
				return 0.5;
			}
			
			@Override
			public String getName() {
				return "Android";
			}
			
			@Override
			public int getAvailableProcessors() {
				return 3;
			}
			
			@Override
			public String getArch() {
				return "ARM";
			}

			@Override
			public long getCommittedVirtualMemorySize() {
				return 3072;
			}

			@Override
			public long getTotalSwapSpaceSize() {
				return 16384;
			}

			@Override
			public long getFreeSwapSpaceSize() {
				return 4096;
			}

			@Override
			public long getProcessCpuTime() {
				return 100000000;
			}

			@Override
			public long getFreePhysicalMemorySize() {
				return 1024;
			}

			@Override
			public long getTotalPhysicalMemorySize() {
				return 8192;
			}			
		});
		register(ManagementFactory.THREAD_MXBEAN_NAME, new ThreadMXBean() {
			final ThreadInfo[] noInfo = new ThreadInfo[0];
			final long[] noIds = new long[0];
			
			@Override
			public void setThreadCpuTimeEnabled(boolean enable) {
			}
			
			@Override
			public void setThreadContentionMonitoringEnabled(boolean enable) {
			}
			
			@Override
			public void resetPeakThreadCount() {
			}
			
			@Override
			public boolean isThreadCpuTimeSupported() {
				return false;
			}
			
			@Override
			public boolean isThreadCpuTimeEnabled() {
				return false;
			}
			
			@Override
			public boolean isThreadContentionMonitoringSupported() {
				return false;
			}
			
			@Override
			public boolean isThreadContentionMonitoringEnabled() {
				return false;
			}
			
			@Override
			public boolean isSynchronizerUsageSupported() {
				return false;
			}
			
			@Override
			public boolean isObjectMonitorUsageSupported() {
				return false;
			}
			
			@Override
			public boolean isCurrentThreadCpuTimeSupported() {
				return false;
			}
			
			@Override
			public long getTotalStartedThreadCount() {
				return 100;
			}
			
			@Override
			public long getThreadUserTime(long id) {
				return 10000000;
			}
			
			@Override
			public ThreadInfo[] getThreadInfo(long[] ids, boolean lockedMonitors,
					boolean lockedSynchronizers) {
				return noInfo;
			}
			
			@Override
			public ThreadInfo[] getThreadInfo(long[] ids, int maxDepth) {
				return noInfo;
			}
			
			@Override
			public ThreadInfo getThreadInfo(long id, int maxDepth) {
				return null;
			}
			
			@Override
			public ThreadInfo[] getThreadInfo(long[] ids) {
				return noInfo;
			}
			
			@Override
			public ThreadInfo getThreadInfo(long id) {
				return null;
			}
			
			@Override
			public long getThreadCpuTime(long id) {
				return 0;
			}
			
			@Override
			public int getThreadCount() {
				return 50;
			}
			
			@Override
			public int getPeakThreadCount() {
				return 100;
			}
			
			@Override
			public int getDaemonThreadCount() {
				return 20;
			}
			
			@Override
			public long getCurrentThreadUserTime() {
				return 1000000;
			}
			
			@Override
			public long getCurrentThreadCpuTime() {
				return 100000;
			}
			
			@Override
			public long[] getAllThreadIds() {
				return noIds;
			}
			
			@Override
			public long[] findMonitorDeadlockedThreads() {
				return noIds;
			}
			
			@Override
			public long[] findDeadlockedThreads() {
				return noIds;
			}
			
			@Override
			public ThreadInfo[] dumpAllThreads(boolean lockedMonitors,
					boolean lockedSynchronizers) {
				return noInfo;
			}
		});
		register(ManagementFactory.CLASS_LOADING_MXBEAN_NAME, new ClassLoadingMXBean() {
			@Override
			public long getTotalLoadedClassCount() {
				return 1000;
			}

			@Override
			public int getLoadedClassCount() {
				return 500;
			}

			@Override
			public long getUnloadedClassCount() {
				return 100;
			}

			@Override
			public boolean isVerbose() {
				return false;
			}

			@Override
			public void setVerbose(boolean value) {
			}			
		});
	}

	private void setupOtherMXBeans() throws JMException {
		register(HOTSPOT_BEAN_NAME, new HotSpotDiagnosticMXBean() {
			@Override
			public void dumpHeap(String arg0, boolean arg1) throws IOException {
			}

			@Override
			public List<VMOption> getDiagnosticOptions() {
				return Collections.emptyList();
			}

			@Override
			public VMOption getVMOption(String arg0) {
				return null;
			}

			@Override
			public void setVMOption(String arg0, String arg1) {
			}			
		});
		register(ManagementFactory.MEMORY_MXBEAN_NAME, new MemoryMXBean() {			
			@Override
			public void setVerbose(boolean value) {
			}
			
			@Override
			public boolean isVerbose() {
				return false;
			}
			
			@Override
			public int getObjectPendingFinalizationCount() {
				return 0;
			}
			
			@Override
			public MemoryUsage getNonHeapMemoryUsage() {
				return new MemoryUsage(200, 300, 500, 900);
			}
			
			@Override
			public MemoryUsage getHeapMemoryUsage() {
				return new MemoryUsage(200, 300, 500, 900);
			}
			
			@Override
			public void gc() {
			}
		});
	}

	private Registry rmiRegistry;
	private InetAddress inetAddress;
	private int serverPort;
	private int registryPort;
	private JMXConnectorServer connector;
	private MBeanServer mbeanServer;
	private int registeredCount;
	private RMIServerSocketFactory serverSocketFactory;
	private boolean serverHostNamePropertySet = false;
	private String serviceUrl;

	/**
	 * Create a JMX server running on a particular registry-port.
	 * 
	 * @param registryPort
	 *            The "RMI registry port" that you specify in jconsole to connect to the server. See
	 *            {@link #setRegistryPort(int)}.
	 */
	public MXBeanServer(int registryPort) {
		this.registryPort = registryPort;
	}

	/**
	 * Create a JMX server running on a particular address and registry-port.
	 * 
	 * @param inetAddress
	 *            Address to bind to. If you use on the non-address constructors, it will bind to all interfaces.
	 * @param registryPort
	 *            The "RMI registry port" that you specify in jconsole to connect to the server. See
	 *            {@link #setRegistryPort(int)}.
	 */
	public MXBeanServer(InetAddress inetAddress, int registryPort) {
		this.inetAddress = inetAddress;
		this.registryPort = registryPort;
	}

	/**
	 * Create a JMX server running on a particular registry and server port pair.
	 * 
	 * @param registryPort
	 *            The "RMI registry port" that you specify in jconsole to connect to the server. See
	 *            {@link #setRegistryPort(int)}.
	 * @param serverPort
	 *            The RMI server port that jconsole uses to transfer data to/from the server. See
	 *            {@link #setServerPort(int)}. The same port as the registry-port can be used.
	 */
	public MXBeanServer(int registryPort, int serverPort) {
		this.registryPort = registryPort;
		this.serverPort = serverPort;
	}

	/**
	 * Create a JMX server running on a particular registry and server port pair.
	 * 
	 * @param inetAddress
	 *            Address to bind to. If you use on the non-address constructors, it will bind to all interfaces.
	 * @param registryPort
	 *            The "RMI registry port" that you specify in jconsole to connect to the server. See
	 *            {@link #setRegistryPort(int)}.
	 * @param serverPort
	 *            The RMI server port that jconsole uses to transfer data to/from the server. See
	 *            {@link #setServerPort(int)}. The same port as the registry-port can be used.
	 */
	public MXBeanServer(InetAddress inetAddress, int registryPort, int serverPort) {
		this.inetAddress = inetAddress;
		this.registryPort = registryPort;
		this.serverPort = serverPort;
	}

	/**
	 * Create a JmxServer wrapper around an existing MBeanServer. You may want to use this with
	 * {@link ManagementFactory#getPlatformMBeanServer()} to use the JVM platform's default server.
	 */
	public MXBeanServer(MBeanServer mbeanServer) {
		this.mbeanServer = mbeanServer;
	}

	/**
	 * Start our JMX service. The port must have already been called either in the {@link #JmxServer(int)} constructor
	 * or the {@link #setRegistryPort(int)} method before this is called.
	 * 
	 * @throws IllegalStateException
	 *             If the registry port has not already been set.
	 */
	public synchronized void start() throws JMException {
		if (mbeanServer != null) {
			// no-op
			return;
		}
		if (registryPort == 0) {
			throw new IllegalStateException("registry-port must be already set when JmxServer is initialized");
		}
		startRmiRegistry();
		startJmxService();
	}

	/**
	 * Same as {@link #stopThrow()} but this ignores any exceptions.
	 */
	public synchronized void stop() {
		try {
			stopThrow();
		} catch (JMException e) {
			// ignored
		}
	}

	/**
	 * Stop the JMX server by closing the connector and unpublishing it from the RMI registry. This throws a JMException
	 * on any issues.
	 */
	public synchronized void stopThrow() throws JMException {
		if (connector != null) {
			try {
				connector.stop();
			} catch (IOException e) {
				throw createJmException("Could not stop our Jmx connector server", e);
			} finally {
				connector = null;
			}
		}
		if (rmiRegistry != null) {
			try {
				UnicastRemoteObject.unexportObject(rmiRegistry, true);
			} catch (NoSuchObjectException e) {
				throw createJmException("Could not unexport our RMI registry", e);
			} finally {
				rmiRegistry = null;
			}
		}
		if (serverHostNamePropertySet) {
			System.clearProperty(RMI_SERVER_HOST_NAME_PROPERTY);
			serverHostNamePropertySet = false;
		}
	}

	public void register(String name, Object mbean) throws JMException {
		System.out.println("Registering "+name);
		register(ObjectName.getInstance(name), mbean);
	}
	
	public void register(ObjectName objectName, Object mbean) throws JMException {
		try {
			mbeanServer.registerMBean(mbean, objectName);
			registeredCount++;
		} catch (Exception e) {
			throw createJmException("Registering JMX object " + objectName + " failed", e);
		}
	}
	
	/**
	 * Same as {@link #unregisterThrow(ObjectName)} except this ignores exceptions.
	 */
	public void unregister(ObjectName objName) {
		try {
			unregisterThrow(objName);
		} catch (Exception e) {
			// ignored
		}
	}

	/**
	 * Un-register the object name from JMX but this throws exceptions. Use the {@link #unregister(Object)} if you want
	 * it to be silent.
	 */
	public synchronized void unregisterThrow(ObjectName objName) throws JMException {
		if (mbeanServer == null) {
			throw new JMException("JmxServer has not be started");
		}
		mbeanServer.unregisterMBean(objName);
		registeredCount--;
	}

	/**
	 * Not required. Default is to bind to local interfaces.
	 */
	public void setInetAddress(InetAddress inetAddress) {
		this.inetAddress = inetAddress;
	}

	/**
	 * This is actually calls {@link #setRegistryPort(int)}.
	 */
	public void setPort(int port) {
		setRegistryPort(port);
	}

	/**
	 * @see JmxServer#setRegistryPort(int)
	 */
	public int getRegistryPort() {
		return registryPort;
	}

	/**
	 * Set our port number to listen for JMX connections. This is the "RMI registry port" but it is the port that you
	 * specify in jconsole to connect to the server. This must be set either here or in the {@link #JmxServer(int)}
	 * constructor before {@link #start()} is called.
	 */
	public void setRegistryPort(int registryPort) {
		this.registryPort = registryPort;
	}

	/**
	 * @see JmxServer#setServerPort(int)
	 */
	public int getServerPort() {
		return serverPort;
	}

	/**
	 * Chances are you should be using {@link #setPort(int)} or {@link #setRegistryPort(int)} unless you know what you
	 * are doing. This sets what JMX calls the "RMI server port". By default this does not have to be set and the
	 * registry-port will be used also as the server-port. Both the registry and the server can be the same port. When
	 * you specify a port number in jconsole this is not the port that should be specified -- see the registry port.
	 */
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	/**
	 * Optional server socket factory that can will be used to generate our registry and server ports. This is not
	 * necessary if you are specifying addresses or ports.
	 */
	public void setServerSocketFactory(RMIServerSocketFactory serverSocketFactory) {
		this.serverSocketFactory = serverSocketFactory;
	}

	/**
	 * Optional service URL which is used to specify the connection endpoints. You should not use this if you are
	 * setting the address or the ports directly. The format is something like:
	 * 
	 * <p>
	 * 
	 * <pre>
	 * service:jmx:rmi://your-server-name:server-port/jndi/rmi://registry-host:registry-port/jmxrmi
	 * </pre>
	 * 
	 * </p>
	 * 
	 * <p>
	 * <tt>your-server-name</tt> could be an IP of an interface or just localhost. <tt>registry-host</tt> can also be an
	 * interface IP or blank for localhost.
	 * </p>
	 */
	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	/**
	 * Number of registered objects.
	 */
	public int getRegisteredCount() {
		return registeredCount;
	}

	private void startRmiRegistry() throws JMException {
		if (rmiRegistry != null) {
			return;
		}
		try {
			if (inetAddress == null) {
				rmiRegistry = LocateRegistry.createRegistry(registryPort);
			} else {
				if (serverSocketFactory == null) {
					serverSocketFactory = new LocalSocketFactory(inetAddress);
				}
				if (System.getProperty(RMI_SERVER_HOST_NAME_PROPERTY) == null) {
					/*
					 * We have to do this because JMX tries to connect back the server that we just set and it won't be
					 * able to locate it if we set our own address to anything but the InetAddress.getLocalHost()
					 * address.
					 */
					System.setProperty(RMI_SERVER_HOST_NAME_PROPERTY, inetAddress.getHostAddress());
					serverHostNamePropertySet = true;
				}
				/*
				 * NOTE: the client factory being null is a critical part of this for some reason. If we specify a
				 * client socket factory then the registry and the RMI server can't be on the same port. Thanks to EJB.
				 * 
				 * I also tried to inject a client socket factory both here and below in the connector environment but I
				 * could not get it to work.
				 */
				rmiRegistry = LocateRegistry.createRegistry(registryPort, null, serverSocketFactory);
			}
		} catch (IOException e) {
			throw createJmException("Unable to create RMI registry on port " + registryPort, e);
		}
	}

	private void startJmxService() throws JMException {
		if (connector != null) {
			return;
		}
		if (serverPort == 0) {
			/*
			 * If we aren't specifying an address then we can use the registry-port for both the registry call _and_ the
			 * RMI calls. There is RMI port multiplexing underneath the covers of the JMX handler. Did not know that.
			 * Thanks to EJB.
			 */
			serverPort = registryPort;
		}
		String serverHost = "localhost";
		String registryHost = "";
		if (inetAddress != null) {
			String hostAddr = inetAddress.getHostAddress();
			serverHost = hostAddr;
			registryHost = hostAddr;
		}
		if (serviceUrl == null) {
			serviceUrl =
					"service:jmx:rmi://" + serverHost + ":" + serverPort + "/jndi/rmi://" + registryHost + ":"
							+ registryPort + "/jmxrmi";
		}
		JMXServiceURL url;
		try {
			url = new JMXServiceURL(serviceUrl);
		} catch (MalformedURLException e) {
			throw createJmException("Malformed service url created " + serviceUrl, e);
		}

		Map<String, Object> envMap = null;
		if (serverSocketFactory != null) {
			envMap = new HashMap<String, Object>();
			envMap.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, serverSocketFactory);
		}
		/*
		 * NOTE: I tried to inject a client socket factory with RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE
		 * but I could not get it to work. It seemed to require the client to have the LocalSocketFactory class in the
		 * classpath.
		 */

		try {
			mbeanServer = getMBeanServer();
			connector = JMXConnectorServerFactory.newJMXConnectorServer(url, envMap, mbeanServer);
		} catch (IOException e) {
			throw createJmException("Could not make our Jmx connector server on URL: " + url, e);
		}
		try {
			connector.start();
		} catch (IOException e) {
			connector = null;
			throw createJmException("Could not start our Jmx connector server on URL: " + url, e);
		}
	}

	private MBeanServer getMBeanServer() {
		if (true) {
			return MBeanServerFactory.createMBeanServer();
		}
		return ManagementFactory.getPlatformMBeanServer();
	}

	private JMException createJmException(String message, Exception e) {
		JMException jmException = new JMException(message);
		jmException.initCause(e);
		return jmException;
	}

	/**
	 * Socket factory which allows us to set a particular local address.
	 */
	private static class LocalSocketFactory implements RMIServerSocketFactory {

		private final InetAddress inetAddress;

		public LocalSocketFactory(InetAddress inetAddress) {
			this.inetAddress = inetAddress;
		}

		public ServerSocket createServerSocket(int port) throws IOException {
			return new ServerSocket(port, 0, inetAddress);
		}

		@Override
		public int hashCode() {
			return (this.inetAddress == null ? 0 : this.inetAddress.hashCode());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			LocalSocketFactory other = (LocalSocketFactory) obj;
			if (this.inetAddress == null) {
				return (other.inetAddress == null);
			} else {
				return this.inetAddress.equals(other.inetAddress);
			}
		}
	}
}
