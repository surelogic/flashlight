package com.surelogic.flashlight.prep.events;

import java.io.IOException;
import java.lang.management.PlatformManagedObject;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIServerSocketFactory;
import java.util.HashMap;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.JMException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;

public class FLMXBeanServer {

    private final String RMI_SERVER_HOST_NAME_PROPERTY = "java.rmi.server.hostname";
    private static final String HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";
    private static final int PORT = 1100;

    private int serverPort;
    private Registry rmiRegistry;
    private InetAddress inetAddress;
    private MBeanServer mbeanServer;
    private RMIServerSocketFactory serverSocketFactory;
    private JMXConnectorServer connector;
    private int registeredCount;
    private final int registryPort;
    private boolean serverHostNamePropertySet = false;
    private String serviceUrl;

    FLMXBeanServer(int registryPort) {
        this.registryPort = registryPort;
    }

    private void setupOtherMXBeans() throws JMException {
        /*
         * register(HOTSPOT_BEAN_NAME, new HotSpotDiagnosticMXBean() {
         *
         * @Override public void dumpHeap(String arg0, boolean arg1) throws
         * IOException {
         *
         * }
         *
         * @Override public List<VMOption> getDiagnosticOptions() { return
         * Collections.emptyList(); }
         *
         * @Override public VMOption getVMOption(String arg0) { return null; }
         *
         * @Override public void setVMOption(String arg0, String arg1) { }
         *
         * @Override public ObjectName getObjectName() { try { return ObjectName
         * .getInstance("com.sun.management:type=HotSpotDiagnostic"); } catch
         * (Exception e) { throw new IllegalStateException(e); } } });
         */
    }

    void setupMXBeans(FLManagementFactory fact) throws JMException {
        setupOtherMXBeans();
        register(fact.getRuntimeMXBean());
        register(fact.getOperatingSystemMXBean());
        register(fact.getThreadMXBean());
        register(fact.getClassLoadingMXBean());

    }

    public void register(String name, Object mbean) throws JMException {
        System.out.println("Registering " + name);
        register(ObjectName.getInstance(name), mbean);
    }

    public void register(ObjectName objectName, Object mbean)
            throws JMException {
        try {
            mbeanServer.registerMBean(mbean, objectName);
            registeredCount++;
        } catch (Exception e) {
            throw createJmException("Registering JMX object " + objectName
                    + " failed", e);
        }
    }

    void register(PlatformManagedObject object)
            throws InstanceAlreadyExistsException, MBeanRegistrationException,
            NotCompliantMBeanException {
        mbeanServer.registerMBean(object, object.getObjectName());
        registeredCount++;
    }

    /**
     * Same as {@link #unregisterThrow(ObjectName)} except this ignores
     * exceptions.
     */
    public void unregister(ObjectName objName) {
        try {
            unregisterThrow(objName);
        } catch (Exception e) {
            // ignored
        }
    }

    /**
     * Un-register the object name from JMX but this throws exceptions. Use the
     * {@link #unregister(Object)} if you want it to be silent.
     */
    public void unregisterThrow(ObjectName objName) throws JMException {
        if (mbeanServer == null) {
            throw new JMException("JmxServer has not be started");
        }
        mbeanServer.unregisterMBean(objName);
        registeredCount--;
    }

    private static JMException createJmException(String message, Exception e) {
        JMException jmException = new JMException(message);
        jmException.initCause(e);
        return jmException;
    }

    /**
     * Start our JMX service. The port must have already been called either in
     * the {@link #JmxServer(int)} constructor or the
     * {@link #setRegistryPort(int)} method before this is called.
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
            throw new IllegalStateException(
                    "registry-port must be already set when JmxServer is initialized");
        }
        startRmiRegistry();
        startJmxService();
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
                     * We have to do this because JMX tries to connect back the
                     * server that we just set and it won't be able to locate it
                     * if we set our own address to anything but the
                     * InetAddress.getLocalHost() address.
                     */
                    System.setProperty(RMI_SERVER_HOST_NAME_PROPERTY,
                            inetAddress.getHostAddress());
                    serverHostNamePropertySet = true;
                }
                /*
                 * NOTE: the client factory being null is a critical part of
                 * this for some reason. If we specify a client socket factory
                 * then the registry and the RMI server can't be on the same
                 * port. Thanks to EJB.
                 *
                 * I also tried to inject a client socket factory both here and
                 * below in the connector environment but I could not get it to
                 * work.
                 */
                rmiRegistry = LocateRegistry.createRegistry(registryPort, null,
                        serverSocketFactory);
            }
        } catch (IOException e) {
            throw createJmException("Unable to create RMI registry on port "
                    + registryPort, e);
        }
    }

    private void startJmxService() throws JMException {
        if (connector != null) {
            return;
        }
        if (serverPort == 0) {
            /*
             * If we aren't specifying an address then we can use the
             * registry-port for both the registry call _and_ the RMI calls.
             * There is RMI port multiplexing underneath the covers of the JMX
             * handler. Did not know that. Thanks to EJB.
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
            serviceUrl = "service:jmx:rmi://" + serverHost + ":" + serverPort
                    + "/jndi/rmi://" + registryHost + ":" + registryPort
                    + "/jmxrmi";
        }
        JMXServiceURL url;
        try {
            url = new JMXServiceURL(serviceUrl);
        } catch (MalformedURLException e) {
            throw createJmException("Malformed service url created "
                    + serviceUrl, e);
        }

        Map<String, Object> envMap = null;
        if (serverSocketFactory != null) {
            envMap = new HashMap<String, Object>();
            envMap.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE,
                    serverSocketFactory);
        }
        /*
         * NOTE: I tried to inject a client socket factory with
         * RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE but I could
         * not get it to work. It seemed to require the client to have the
         * LocalSocketFactory class in the classpath.
         */

        try {
            mbeanServer = getMBeanServer();
            connector = JMXConnectorServerFactory.newJMXConnectorServer(url,
                    envMap, mbeanServer);
        } catch (IOException e) {
            throw createJmException(
                    "Could not make our Jmx connector server on URL: " + url, e);
        }
        try {
            connector.start();
        } catch (IOException e) {
            connector = null;
            throw createJmException(
                    "Could not start our Jmx connector server on URL: " + url,
                    e);
        }
    }

    private MBeanServer getMBeanServer() {
        return MBeanServerFactory.createMBeanServer();
    }

    /**
     * Socket factory which allows us to set a particular local address.
     */
    private static class LocalSocketFactory implements RMIServerSocketFactory {

        private final InetAddress inetAddress;

        public LocalSocketFactory(InetAddress inetAddress) {
            this.inetAddress = inetAddress;
        }

        @Override
        public ServerSocket createServerSocket(int port) throws IOException {
            return new ServerSocket(port, 0, inetAddress);
        }

        @Override
        public int hashCode() {
            return inetAddress == null ? 0 : inetAddress.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            LocalSocketFactory other = (LocalSocketFactory) obj;
            if (inetAddress == null) {
                return other.inetAddress == null;
            } else {
                return inetAddress.equals(other.inetAddress);
            }
        }
    }
}
