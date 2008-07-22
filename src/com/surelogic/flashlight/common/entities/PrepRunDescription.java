package com.surelogic.flashlight.common.entities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.surelogic.common.i18n.I18N;
import com.surelogic.flashlight.common.model.RunDescription;

/**
 * A description for a prepared run. This class is immutable and has value
 * semantics.
 */
public final class PrepRunDescription {

	private final int f_run;

	public int getRun() {
		return f_run;
	}

	private final RunDescription f_description;

	public RunDescription getDescription() {
		return f_description;
	}

	public PrepRunDescription(final int run, final RunDescription description) {
		f_run = run;
		if (description == null)
			throw new IllegalArgumentException(I18N.err(44, "description"));
		f_description = description;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("[PrepRunDescription: run=").append(f_run);
		b.append(" description=").append(f_description);
		b.append("]");
		return b.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((f_description == null) ? 0 : f_description.hashCode());
		result = prime * result + f_run;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PrepRunDescription other = (PrepRunDescription) obj;
		if (f_description == null) {
			if (other.f_description != null)
				return false;
		} else if (!f_description.equals(other.f_description))
			return false;
		if (f_run != other.f_run)
			return false;
		return true;
	}

	/**
	 * Extracts the set of descriptions from a collection of prepared run
	 * descriptions.
	 * 
	 * @param prepRunDescriptions
	 *            the collection to extract the descriptions from.
	 * @return the set of descriptions extracted.
	 */
	public static Set<RunDescription> getDescriptions(
			final Collection<PrepRunDescription> prepRunDescriptions) {
		final Set<RunDescription> result = new HashSet<RunDescription>();
		for (PrepRunDescription d : prepRunDescriptions) {
			result.add(d.getDescription());
		}
		return result;
	}
}
