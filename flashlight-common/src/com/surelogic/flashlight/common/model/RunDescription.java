package com.surelogic.flashlight.common.model;

import java.sql.Timestamp;

import com.surelogic.Immutable;
import com.surelogic.ValueObject;
import com.surelogic.common.i18n.I18N;

/**
 * Describes the run of a program with Flashlight instrumentation.
 */
@Immutable
@ValueObject
public final class RunDescription {

  public RunDescription(final String name, final String rawDataVersion, final String hostname, final String userName,
      final String javaVersion, final String javaVendor, final String osName, final String osArch, final String osVersion,
      final int maxMemoryMb, final int processors, final Timestamp started, final long duration, final boolean isAndroid,
      final boolean completed) {
    if (name == null) {
      throw new IllegalArgumentException(I18N.err(44, "name"));
    }
    f_name = name;
    if (rawDataVersion == null) {
      throw new IllegalArgumentException(I18N.err(44, "rawDataVersion"));
    }
    f_rawDataVersion = rawDataVersion;
    /*
     * The hostname and username fields were added, so we will accept entries
     * that do not have them by using a reasonable default.
     */
    if (hostname == null) {
      f_hostname = "unknown";
    } else {
      f_hostname = hostname;
    }
    if (userName == null) {
      f_userName = "unknown";
    } else {
      f_userName = userName;
    }
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
    f_duration = duration;
    f_android = isAndroid;
    f_completed = completed;
  }

  private final String f_name;

  public String getName() {
    return f_name;
  }

  private final String f_rawDataVersion;

  public String getRawDataVersion() {
    return f_rawDataVersion;
  }

  private final String f_hostname;

  public String getHostname() {
    return f_hostname;
  }

  private final String f_userName;

  public String getUserName() {
    return f_userName;
  }

  private final String f_javaVersion;

  public String getJavaVersion() {
    return f_javaVersion;
  }

  private final String f_javaVendor;

  public String getJavaVendor() {
    return f_javaVendor;
  }

  private final String f_osName;

  public String getOSName() {
    return f_osName;
  }

  private final String f_osArch;

  public String getOSArch() {
    return f_osArch;
  }

  private final String f_osVersion;

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

  private final Timestamp f_started;

  public Timestamp getStartTimeOfRun() {
    return f_started;
  }

  private final long f_duration;

  public long getDuration() {
    return f_duration;
  }

  private final boolean f_android;

  public boolean isAndroid() {
    return f_android;
  }

  private final boolean f_completed;

  public boolean isCompleted() {
    return f_completed;
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
    b.append(" duration=").append(f_duration);
    b.append(" isAndroid=").append(f_android);
    b.append(" completed=").append(f_completed);
    b.append("]");
    return b.toString();
  }

  /**
   * Returns a string that can be used to identify this run description. This
   * call is a shortcut for the below code.
   * 
   * <pre>
   * getName() + &quot; - &quot; + getStartTimeOfRun()
   * </pre>
   * 
   * @return a string that can be used to identify this run description.
   */
  public String toIdentityString() {
    return getName() + " - " + getStartTimeOfRun();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (f_android ? 1231 : 1237);
    result = prime * result + (f_completed ? 1231 : 1237);
    result = prime * result + (int) (f_duration ^ (f_duration >>> 32));
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
    if (f_completed != other.f_completed)
      return false;
    if (f_duration != other.f_duration)
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
