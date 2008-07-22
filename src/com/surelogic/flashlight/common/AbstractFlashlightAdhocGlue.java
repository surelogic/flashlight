package com.surelogic.flashlight.common;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;

import com.surelogic.adhoc.IAdHoc;
import com.surelogic.common.FileUtility;

public abstract class AbstractFlashlightAdhocGlue implements IAdHoc {

	public final Connection getConnection() throws SQLException {
		return Data.getInstance().getConnection();
	}

	public File getQuerySaveFile() {
		File qsf = new File(FileUtility.getFlashlightDataDirectory()
				+ File.separator + "queries.xml");
		if (!qsf.exists()) {
			FileUtility.copy(Data.getDefaultQueryFileURL(), qsf);
		}
		return qsf;
	}

	public Executor getExecutor() {
		return Data.getInstance().getExecutor();
	}
}
