package com.surelogic.flashlight.prep.events;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.List;

public class FLManagementFactory {

    private final FLClassLoadingMXBean clBean;
    private final FLOSMXBean osBean;
    private final FLRuntimeMxBean rtBean;
    private final FLThreadMxBean thBean;

    private final FLMemoryMXBean memBean;
    private final FLMemoryPoolMXBean mpBean;
    private final FLMemoryManagerMXBean mmBean;
    private final FLGarbageCollectorMXBean gcBean;

    private FLManagementFactory(FLClassLoadingMXBean clBean, FLOSMXBean osBean,
            FLRuntimeMxBean rtBean, FLThreadMxBean thBean,
            FLMemoryMXBean memBean, FLMemoryPoolMXBean mpBean,
            FLMemoryManagerMXBean mmBean, FLGarbageCollectorMXBean gcBean) {
        this.clBean = clBean;
        this.osBean = osBean;
        this.rtBean = rtBean;
        this.thBean = thBean;
        this.memBean = memBean;
        this.mpBean = mpBean;
        this.mmBean = mmBean;
        this.gcBean = gcBean;
    }

    /**
     * Returns the managed bean for the class loading system of the Java virtual
     * machine.
     *
     * @return a {@link ClassLoadingMXBean} object for the Java virtual machine.
     */
    public ClassLoadingMXBean getClassLoadingMXBean() {
        return clBean;
    }

    /**
     * Returns the managed bean for the memory system of the Java virtual
     * machine.
     *
     * @return a {@link MemoryMXBean} object for the Java virtual machine.
     */
    public MemoryMXBean getMemoryMXBean() {
        return memBean;
    }

    /**
     * Returns the managed bean for the thread system of the Java virtual
     * machine.
     *
     * @return a {@link ThreadMXBean} object for the Java virtual machine.
     */
    public ThreadMXBean getThreadMXBean() {
        return thBean;
    }

    /**
     * Returns the managed bean for the runtime system of the Java virtual
     * machine.
     *
     * @return a {@link RuntimeMXBean} object for the Java virtual machine.
     */
    public RuntimeMXBean getRuntimeMXBean() {
        return rtBean;
    }

    /**
     * Returns the managed bean for the compilation system of the Java virtual
     * machine. This method returns <tt>null</tt> if the Java virtual machine
     * has no compilation system.
     *
     * @return a {@link CompilationMXBean} object for the Java virtual machine
     *         or <tt>null</tt> if the Java virtual machine has no compilation
     *         system.
     */
    public CompilationMXBean getCompilationMXBean() {
        return null;
    }

    /**
     * Returns the managed bean for the operating system on which the Java
     * virtual machine is running.
     *
     * @return an {@link OperatingSystemMXBean} object for the Java virtual
     *         machine.
     */
    public OperatingSystemMXBean getOperatingSystemMXBean() {
        return osBean;
    }

    /**
     * Returns a list of {@link MemoryPoolMXBean} objects in the Java virtual
     * machine. The Java virtual machine can have one or more memory pools. It
     * may add or remove memory pools during execution.
     *
     * @return a list of <tt>MemoryPoolMXBean</tt> objects.
     *
     */
    public List<MemoryPoolMXBean> getMemoryPoolMXBeans() {
        return Collections.singletonList((MemoryPoolMXBean) mpBean);
    }

    /**
     * Returns a list of {@link MemoryManagerMXBean} objects in the Java virtual
     * machine. The Java virtual machine can have one or more memory managers.
     * It may add or remove memory managers during execution.
     *
     * @return a list of <tt>MemoryManagerMXBean</tt> objects.
     *
     */
    public List<MemoryManagerMXBean> getMemoryManagerMXBeans() {
        return Collections.singletonList((MemoryManagerMXBean) mmBean);
    }

    /**
     * Returns a list of {@link GarbageCollectorMXBean} objects in the Java
     * virtual machine. The Java virtual machine may have one or more
     * <tt>GarbageCollectorMXBean</tt> objects. It may add or remove
     * <tt>GarbageCollectorMXBean</tt> during execution.
     *
     * @return a list of <tt>GarbageCollectorMXBean</tt> objects.
     *
     */
    public List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
        return Collections.singletonList((GarbageCollectorMXBean) gcBean);
    }

}
