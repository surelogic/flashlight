package com.surelogic.flashlight.common.jobs;

import java.io.File;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;

import com.surelogic._flashlight.common.BinaryEventReader;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.common.license.SLLicenseUtility;
import com.surelogic.flashlight.common.convert.ConvertBinaryFileScan;
import com.surelogic.flashlight.common.files.RawDataFilePrefix;
import com.surelogic.flashlight.common.files.RawFileUtility;

public final class ConvertBinaryToXMLJob extends AbstractSLJob {
	private final File f_dataFile;

	/**
	 * Constructs a job instance to convert the passed raw data file into a
	 * compressed XML file.
	 * 
	 * @param dataFile
	 *            a raw data file, either <tt>.flb</tt> or <tt>.flb.gz</tt>.
	 */
	public ConvertBinaryToXMLJob(final File dataFile) {
		super("Converting " + dataFile.getName());
		f_dataFile = dataFile;
	}

	public SLStatus run(final SLProgressMonitor monitor) {
		final String dataFileName = f_dataFile.getName();
		monitor.begin();
		try {
			final SLStatus failed = SLLicenseUtility.validateSLJob(
					SLLicenseProduct.FLASHLIGHT, monitor);
			if (failed != null)
				return failed;

			final RawDataFilePrefix rawFilePrefix = RawFileUtility
					.getPrefixFor(f_dataFile);
			final InputStream stream = RawFileUtility
					.getInputStreamFor(f_dataFile);
			try {
				final String newName = rawFilePrefix.getName() + ".fl.txt.gz";
				final File convertedFile = new File(f_dataFile.getParentFile(),
						newName);
				final ConvertBinaryFileScan convertFile = new ConvertBinaryFileScan(
						convertedFile);
				final SAXParser saxParser = RawFileUtility
						.getParser(f_dataFile);
				if (saxParser instanceof BinaryEventReader) {
					BinaryEventReader r = (BinaryEventReader) saxParser;
					r.showRawData(true);
				}
				saxParser.parse(stream, convertFile);
				convertFile.close();
				stream.close();

				if (monitor.isCanceled()) {
					return SLStatus.CANCEL_STATUS;
				}
				return SLStatus.OK_STATUS;
			} finally {
				stream.close();
			}
		} catch (final Exception e) {
			/*
			 * We check for a cancel here because a SAXException is thrown out
			 * of the parser when the user presses cancel.
			 */
			if (monitor.isCanceled()) {
				return SLStatus.CANCEL_STATUS;
			}
			final int code = 116;
			final String msg = I18N.err(code, dataFileName);
			return SLStatus.createErrorStatus(code, msg, e);
		} finally {
			monitor.done();
		}
	}
}
