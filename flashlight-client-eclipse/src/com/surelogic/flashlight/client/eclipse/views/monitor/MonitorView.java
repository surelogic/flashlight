package com.surelogic.flashlight.client.eclipse.views.monitor;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.CommonImages;
import com.surelogic.common.XUtil;
import com.surelogic.common.ui.SLImages;

public class MonitorView extends ViewPart {

	private MonitorViewMediator f_mediator;

	@Override
	public void createPartControl(final Composite parent) {
		final GridLayout layout = new GridLayout();
		parent.setLayout(layout);
		final GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		parent.setLayoutData(layoutData);

		/*
		 * Status Bar
		 */
		final Composite status = new Composite(parent, SWT.NONE);
		final GridLayout statusLayout = new GridLayout();
		statusLayout.marginHeight = 0;
		statusLayout.marginWidth = 0;
		statusLayout.numColumns = 3;
		statusLayout.verticalSpacing = 0;
		status.setLayout(statusLayout);
		final GridData statusLayoutData = new GridData(SWT.FILL, SWT.FILL,
				true, false);
		status.setLayoutData(statusLayoutData);

		final Label statusImage = new Label(status, SWT.NONE);
		statusImage.setImage(SLImages.getImage(CommonImages.IMG_GRAY_CIRCLE));

		final Label startTimeText = new Label(status, SWT.NONE);
		final Label runText = new Label(status, SWT.NONE);

		/*
		 * Tab folder
		 */
		final TabFolder folder = new TabFolder(parent, SWT.NONE);
		folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		/*
		 * Fields Tab
		 */
		final TabItem fieldsTab = new TabItem(folder, SWT.NONE);
		fieldsTab.setText("Fields");
		final Composite fieldsBody = new Composite(folder, SWT.NONE);
		fieldsBody.setLayout(new GridLayout());
		fieldsBody.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		final Composite fieldsSelector = new Composite(fieldsBody, SWT.NONE);
		GridLayout fsLayout = new GridLayout();
		fsLayout.marginWidth = 0;
		fsLayout.marginHeight = 0;
		fsLayout.numColumns = 2;
		fieldsSelector.setLayout(fsLayout);
		fieldsSelector.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));

		final Text fieldsSelectorText = new Text(fieldsSelector, SWT.SINGLE);
		fieldsSelectorText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		final Button fieldsSelectorButton = new Button(fieldsSelector, SWT.PUSH);
		fieldsSelectorButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
				false, false));
		fieldsSelectorButton.setText("Set");
		fieldsSelectorButton.setEnabled(false);
		fieldsSelectorText.setEnabled(false);

		final Tree fieldsTree = new Tree(fieldsBody, SWT.VIRTUAL);
		fieldsTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		fieldsTab.setControl(fieldsBody);
		fieldsBody.layout();

		/*
		 * Locks Tab
		 */
		final TabItem locksTab = new TabItem(folder, SWT.NONE);
		locksTab.setText("Locks");

		Composite locksBody = new Composite(folder, SWT.NONE);
		locksBody.setLayout(new GridLayout());
		locksBody.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		final Tree locksTree = new Tree(locksBody, SWT.VIRTUAL);
		locksTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		locksTab.setControl(locksBody);

		/*
		 * EDT Tab
		 */
		final TabItem edtTab = new TabItem(folder, SWT.NONE);
		edtTab.setText("EDT");

		final Composite edtBody = new Composite(folder, SWT.NONE);
		edtBody.setLayout(new GridLayout());
		edtBody.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		final Composite edtSelector = new Composite(edtBody, SWT.NONE);
		GridLayout edtLayout = new GridLayout();
		edtLayout.numColumns = 2;
		edtLayout.marginHeight = 0;
		edtLayout.marginWidth = 0;
		edtSelector.setLayout(edtLayout);
		edtSelector
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		final Text edtSelectorText = new Text(edtSelector, SWT.SINGLE);
		edtSelectorText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		final Button edtSelectorButton = new Button(edtSelector, SWT.PUSH);
		edtSelectorButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false,
				false));
		edtSelectorButton.setText("Set");
		edtSelectorButton.setEnabled(false);
		edtSelectorText.setEnabled(false);

		final Tree edtTree = new Tree(edtBody, SWT.NONE);
		edtTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		edtTab.setControl(edtBody);

		/*
		 * Listing tab, we aren't keeping this.
		 */
		TextViewer tv = null;
		if (XUtil.useExperimental()) {
			final TabItem removeThis = new TabItem(folder, SWT.NONE);
			removeThis.setText("List");
			tv = new TextViewer(folder, SWT.BORDER | SWT.V_SCROLL
					| SWT.H_SCROLL);
			removeThis.setControl(tv.getControl());
		}
		final IActionBars actionBars = getViewSite().getActionBars();
		final IMenuManager menu = actionBars.getMenuManager();
		menu.add(new ConnectToRunningMonitorAction());

		f_mediator = new MonitorViewMediator(status, statusImage, runText,
				startTimeText, fieldsSelectorText, fieldsSelectorButton,
				fieldsTree, locksTree, edtSelectorText, edtSelectorButton,
				edtTree, tv);

		f_mediator.init();
	}

	@Override
	public void dispose() {
		/*
		 * We need to check for null because Eclipse gives us no guarantee that
		 * createPartControl() was called.
		 */
		if (f_mediator != null) {
			f_mediator.dispose();
			f_mediator = null;
		}
		super.dispose();
	}

	@Override
	public void setFocus() {
		/*
		 * This is a good point to refresh the contents of the view.
		 */
		if (f_mediator != null) {
			f_mediator.setFocus();
		}
	}
}
