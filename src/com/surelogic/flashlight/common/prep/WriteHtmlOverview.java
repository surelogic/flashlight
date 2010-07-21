package com.surelogic.flashlight.common.prep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;

import com.surelogic.common.SLUtility;
import com.surelogic.common.html.SimpleHTMLPrinter;
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
			final String styleSheet = loadStyleSheet();
			final StringBuilder b = new StringBuilder();
			SimpleHTMLPrinter.addPageProlog(b, styleSheet);
			/*
			 * TODO Add real page information/generation/read from a file here.
			 */
			b.append("<h1>").append(f_runDescription.getName()).append(' ');
			b.append(SLUtility
					.toStringHMS(f_runDescription.getStartTimeOfRun()));
			b.append("</h1>");

			b
					.append("There are <a href=\"index.html?query=bd72a5e4-42aa-415d-aa72-28d351f629a4\">shared instance fields</a>.");

			SimpleHTMLPrinter.addPageEpilog(b);

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

	/**
	 * Loads and returns the Javadoc hover style sheet.
	 * 
	 * @return the style sheet, or <code>null</code> if unable to load
	 */
	private static String loadStyleSheet() {
		final URL styleSheetURL = Thread
				.currentThread()
				.getContextClassLoader()
				.getResource(
						"/com/surelogic/flashlight/common/prep/RunOverviewStyleSheet.css");
		if (styleSheetURL != null) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(styleSheetURL
						.openStream()));
				final StringBuffer buffer = new StringBuffer(1500);
				String line = reader.readLine();
				while (line != null) {
					buffer.append(line);
					buffer.append('\n');
					line = reader.readLine();
				}
				return buffer.toString();
			} catch (final IOException ex) {
				throw new IllegalStateException(ex);
			} finally {
				try {
					if (reader != null) {
						reader.close();
					}
				} catch (final IOException e) {
				}
			}
		}
		return null;
	}
}
