/**
 * 
 */
package com.surelogic.flashlight.recommend;

public final class FieldLoc implements Comparable<FieldLoc> {
	private final String field;
	private final Visibility visibility;
	private final boolean isFinal;
	private final boolean isStatic;
	private final boolean isVolatile;

	FieldLoc(final String field, final Visibility visibility,
			final boolean isStatic, final boolean isFinal,
			final boolean isVolatile) {
		this.field = field;
		this.visibility = visibility;
		this.isFinal = isFinal;
		this.isStatic = isStatic;
		this.isVolatile = isVolatile;
	}

	@Override
	public String toString() {
		return field;
	}

	public String getField() {
		return field;
	}

	public boolean isFinal() {
		return isFinal;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public boolean isVolatile() {
		return isVolatile;
	}

	public Visibility getVisibility() {
		return visibility;
	}

	public int compareTo(final FieldLoc o) {
		return field.compareTo(o.field);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (field == null ? 0 : field.hashCode());
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
		final FieldLoc other = (FieldLoc) obj;
		if (field == null) {
			if (other.field != null) {
				return false;
			}
		} else if (!field.equals(other.field)) {
			return false;
		}
		return true;
	}

}