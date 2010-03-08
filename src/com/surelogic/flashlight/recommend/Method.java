/**
 * 
 */
package com.surelogic.flashlight.recommend;

public final class Method {
	private final String pakkage;
	private final String clazz;
	private final String loc;

	public Method(final String p, final String c, final String l) {
		this.pakkage = p;
		this.clazz = c;
		this.loc = l;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (clazz == null ? 0 : clazz.hashCode());
		result = prime * result + (loc == null ? 0 : loc.hashCode());
		result = prime * result
				+ (pakkage == null ? 0 : pakkage.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Method other = (Method) obj;
		if (clazz == null) {
			if (other.clazz != null) {
				return false;
			}
		} else if (!clazz.equals(other.clazz)) {
			return false;
		}
		if (loc == null) {
			if (other.loc != null) {
				return false;
			}
		} else if (!loc.equals(other.loc)) {
			return false;
		}
		if (pakkage == null) {
			if (other.pakkage != null) {
				return false;
			}
		} else if (!pakkage.equals(other.pakkage)) {
			return false;
		}
		return true;
	}

}