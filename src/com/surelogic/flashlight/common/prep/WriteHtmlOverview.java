package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;

import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.flashlight.common.files.HtmlHandles;
import com.surelogic.flashlight.common.model.RunDescription;

public final class WriteHtmlOverview implements IPostPrep {

	private final RunDescription f_runDescription;

	public WriteHtmlOverview(final RunDescription runDescription) {
		if (runDescription == null)
			throw new IllegalArgumentException(I18N.err(44, "runDescription"));
		f_runDescription = runDescription;
	}

	public void doPostPrep(Connection c, SLProgressMonitor mon)
			throws SQLException {
		mon.begin();
		try {
			final StringBuilder b = new StringBuilder();
			/*
			 * TODO Add real page information/generation/read from a file here.
			 */
			b.append("<html><body>");
			b.append("<h4>").append(f_runDescription.getName()).append(' ');
			b.append(SLUtility
					.toStringHMS(f_runDescription.getStartTimeOfRun()));
			b.append("</h4>");

			b
					.append("This is a <a href=\".#run_query_1\">link</a> to nowhere.");

			b.append("</body></html>");

			final HtmlHandles html = f_runDescription.getRunDirectory()
					.getHtmlHandles();
			html.writeIndexHtml(b.toString());
		} finally {
			mon.done();
		}
	}

	public String getDescription() {
		return "Generating an HTML overview of the results of this run";
	}
}
