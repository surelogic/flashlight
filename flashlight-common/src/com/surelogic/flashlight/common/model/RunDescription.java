package com.surelogic.flashlight.common.model;

import java.sql.Timestamp;

import com.surelogic.Immutable;
import com.surelogic.NonNull;
import com.surelogic.ValueObject;
import com.surelogic.common.i18n.I18N;

/**
 * Describes a run of a Flashlight-instrumented program that has completed data
 * collection. This class is used to see if the information loaded about two run
 * directories is really the same because it is a value object.
 */
@Immutable
@ValueObject
public final class RunDescription {

  /**
   * Obtains a run description for the passed raw file prefix or throws an
   * exception.
   * 
   * @param prefix
   *          a well-formed checkpoint data file prefix.
   * @param collectionDurationNanos
   *          the duration of collection in nanoseconds. This is critical to
   *          check if more raw data somehow appeared in the directory.
   * @return a run description based upon the passed prefix info.
   * @throws Exception
   *           if something goes wrong.
   */
  @NonNull
  public static RunDescription getInstance(final CheckpointFilePrefix prefix, final long collectionDurationNanos) {
    if (prefix == null)
      throw new IllegalArgumentException(I18N.err(44, "prefix"));

    if (prefix.isWellFormed()) {
      return new RunDescription(prefix.getName(), prefix.getRawDataVersion(), prefix.getHostname(), prefix.getUserName(),
          prefix.getJavaVersion(), prefix.getJavaVendor(), prefix.getOSName(), prefix.getOSArch(), prefix.getOSVersion(),
          prefix.getMaxMemoryMb(), prefix.getProcessors(), new Timestamp(prefix.getWallClockTime().getTime()), prefix.isAndroid(),
          collectionDurationNanos);
    } else {
      throw new IllegalStateException(I18N.err(107, prefix.getFile().getAbsolutePath()));
    }
  }

  private RunDescription(final String name, final String rawDataVersion, final String hostname, final String userName,
      final String javaVersion, final String javaVendor, final String osName, final String osArch, final String osVersion,
      final int maxMemoryMb, final int processors, final Timestamp started, final boolean isAndroid,
      final long collectionDurationNanos) {
    if (name == null) {
      throw new IllegalArgumentException(I18N.err(44, "name"));
    }
    f_name = name;
    if (rawDataVersion == null) {
      throw new IllegalArgumentException(I18N.err(44, "rawDataVersion"));
    }
    f_rawDataVersion = rawDataVersion;
    if (hostname == null) {
      throw new IllegalArgumentException(I18N.err(44, "hostname"));
    }
    f_hostname = hostname;
    if (userName == null) {
      throw new IllegalArgumentException(I18N.err(44, "userName"));
    }
    f_userName = userName;
    if (javaVersion == null) {
      throw new IllegalArgumentException(I18N.err(44, "javaVersion"));
    }
    f_javaVersion = javaVersion;
    if (javaVendor == null) {
      throw new IllegalArgumentException(I18N.err(44, "javaVendor"));
    }
    f_javaVendor = javaVendor;
    if (osName == null) {
      throw new IllegalArgumentException(I18N.err(44, "osName"));
    }
    f_osName = osName;
    if (osArch == null) {
      throw new IllegalArgumentException(I18N.err(44, "osArch"));
    }
    f_osArch = osArch;
    if (osVersion == null) {
      throw new IllegalArgumentException(I18N.err(44, "osVersion"));
    }
    f_osVersion = osVersion;
    f_maxMemoryMb = maxMemoryMb;
    f_processors = processors;
    if (started == null) {
      throw new IllegalArgumentException(I18N.err(44, "started"));
    }
    f_started = started;
    f_android = isAndroid;
    f_collectionDurationNanos = collectionDurationNanos;
  }

  @NonNull
  private final String f_name;

  @NonNull
  public String getName() {
    return f_name;
  }

  @NonNull
  public String getSimpleName() {
    return FlashlightFileUtility.getSimpleRunName(f_name);
  }

  @NonNull
  private final String f_rawDataVersion;

  @NonNull
  public String getRawDataVersion() {
    return f_rawDataVersion;
  }

  @NonNull
  private final String f_hostname;

  @NonNull
  public String getHostname() {
    return f_hostname;
  }

  @NonNull
  private final String f_userName;

  @NonNull
  public String getUserName() {
    return f_userName;
  }

  @NonNull
  private final String f_javaVersion;

  @NonNull
  public String getJavaVersion() {
    return f_javaVersion;
  }

  @NonNull
  private final String f_javaVendor;

  @NonNull
  public String getJavaVendor() {
    return f_javaVendor;
  }

  @NonNull
  private final String f_osName;

  @NonNull
  public String getOSName() {
    return f_osName;
  }

  @NonNull
  private final String f_osArch;

  @NonNull
  public String getOSArch() {
    return f_osArch;
  }

  @NonNull
  private final String f_osVersion;

  @NonNull
  public String getOSVersion() {
    return f_osVersion;
  }

  private final int f_maxMemoryMb;

  public int getMaxMemoryMb() {
    return f_maxMemoryMb;
  }

  private final int f_processors;

  public int getProcessors() {
    return f_processors;
  }

  @NonNull
  private final Timestamp f_started;

  @NonNull
  public Timestamp getStartTimeOfRun() {
    return f_started;
  }

  private final boolean f_android;

  public boolean isAndroid() {
    return f_android;
  }

  private final long f_collectionDurationNanos;

  public long getCollectionDurationInNanos() {
    return f_collectionDurationNanos;
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder();
    b.append("[RunDescription: name=").append(f_name);
    b.append(" rawDataVersion=").append(f_rawDataVersion);
    b.append(" hostname=").append(f_hostname);
    b.append(" userName=").append(f_userName);
    b.append(" javaVendor=").append(f_javaVendor);
    b.append(" javaVersion=").append(f_javaVersion);
    b.append(" osName=").append(f_osName);
    b.append(" osArch=").append(f_osArch);
    b.append(" osVersion=").append(f_osVersion);
    b.append(" Max Memory: ").append(f_maxMemoryMb).append(" MB");
    b.append(" processors=").append(f_processors);
    b.append(" started=").append(f_started);
    b.append(" duration=").append(f_collectionDurationNanos);
    b.append(" isAndroid=").append(f_android);
    b.append("]");
    return b.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (f_android ? 1231 : 1237);
    result = prime * result + (int) (f_collectionDurationNanos ^ (f_collectionDurationNanos >>> 32));
    result = prime * result + ((f_hostname == null) ? 0 : f_hostname.hashCode());
    result = prime * result + ((f_javaVendor == null) ? 0 : f_javaVendor.hashCode());
    result = prime * result + ((f_javaVersion == null) ? 0 : f_javaVersion.hashCode());
    result = prime * result + f_maxMemoryMb;
    result = prime * result + ((f_name == null) ? 0 : f_name.hashCode());
    result = prime * result + ((f_osArch == null) ? 0 : f_osArch.hashCode());
    result = prime * result + ((f_osName == null) ? 0 : f_osName.hashCode());
    result = prime * result + ((f_osVersion == null) ? 0 : f_osVersion.hashCode());
    result = prime * result + f_processors;
    result = prime * result + ((f_rawDataVersion == null) ? 0 : f_rawDataVersion.hashCode());
    result = prime * result + ((f_started == null) ? 0 : f_started.hashCode());
    result = prime * result + ((f_userName == null) ? 0 : f_userName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof RunDescription))
      return false;
    RunDescription other = (RunDescription) obj;
    if (f_android != other.f_android)
      return false;
    if (f_collectionDurationNanos != other.f_collectionDurationNanos)
      return false;
    if (f_hostname == null) {
      if (other.f_hostname != null)
        return false;
    } else if (!f_hostname.equals(other.f_hostname))
      return false;
    if (f_javaVendor == null) {
      if (other.f_javaVendor != null)
        return false;
    } else if (!f_javaVendor.equals(other.f_javaVendor))
      return false;
    if (f_javaVersion == null) {
      if (other.f_javaVersion != null)
        return false;
    } else if (!f_javaVersion.equals(other.f_javaVersion))
      return false;
    if (f_maxMemoryMb != other.f_maxMemoryMb)
      return false;
    if (f_name == null) {
      if (other.f_name != null)
        return false;
    } else if (!f_name.equals(other.f_name))
      return false;
    if (f_osArch == null) {
      if (other.f_osArch != null)
        return false;
    } else if (!f_osArch.equals(other.f_osArch))
      return false;
    if (f_osName == null) {
      if (other.f_osName != null)
        return false;
    } else if (!f_osName.equals(other.f_osName))
      return false;
    if (f_osVersion == null) {
      if (other.f_osVersion != null)
        return false;
    } else if (!f_osVersion.equals(other.f_osVersion))
      return false;
    if (f_processors != other.f_processors)
      return false;
    if (f_rawDataVersion == null) {
      if (other.f_rawDataVersion != null)
        return false;
    } else if (!f_rawDataVersion.equals(other.f_rawDataVersion))
      return false;
    if (f_started == null) {
      if (other.f_started != null)
        return false;
    } else if (!f_started.equals(other.f_started))
      return false;
    if (f_userName == null) {
      if (other.f_userName != null)
        return false;
    } else if (!f_userName.equals(other.f_userName))
      return false;
    return true;
  }
}
