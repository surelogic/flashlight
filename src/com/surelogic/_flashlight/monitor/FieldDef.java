/**
 * 
 */
package com.surelogic._flashlight.monitor;

final class FieldDef {
	private final long id;
	private final String clazz;
	private final String field;
	private final boolean isStatic;
	private final boolean isFinal;
	private final boolean isVolatile;

	FieldDef(final int id, final String clazz, final String field,
			final boolean isStatic, final boolean isFinal,
			final boolean isVolatile) {
		this.id = id;
		this.clazz = clazz;
		this.field = field;
		this.isStatic = isStatic;
		this.isFinal = isFinal;
		this.isVolatile = isVolatile;
	}

	public long getId() {
		return id;
	}

	public String getClazz() {
		return clazz;
	}

	public String getField() {
		return field;
	}

	public String getQualifiedFieldName() {
		return clazz + "." + field;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public boolean isFinal() {
		return isFinal;
	}

	public boolean isVolatile() {
		return isVolatile;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ id >>> 32);
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
		final FieldDef other = (FieldDef) obj;
		if (id != other.id) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return getQualifiedFieldName();
	}
}