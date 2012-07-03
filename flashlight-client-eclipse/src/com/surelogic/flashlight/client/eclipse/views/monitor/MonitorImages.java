package com.surelogic.flashlight.client.eclipse.views.monitor;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;

public class MonitorImages {

    private final Image f_package;
    private final Image f_class;
    private final Image f_connecting;
    private final Image f_notConnected;
    private final Image f_connected;
    private final Image f_error;
    private final Image f_done;

    private final Color f_edtAlertColor;
    private final Color f_protectedColor;
    private final Color f_sharedColor;
    private final Color f_dataRaceColor;
    private final Color f_unknownColor;
    private final Color f_unsharedColor;

    public MonitorImages(final Display display) {
        f_connected = SLImages.getImage(CommonImages.IMG_GREEN_CIRCLE);
        f_connecting = SLImages.getImage(CommonImages.IMG_YELLOW_CIRCLE);
        f_notConnected = SLImages.getImage(CommonImages.IMG_GRAY_CIRCLE);
        f_error = SLImages.getImage(CommonImages.IMG_RED_CIRCLE);
        f_done = SLImages.getImage(CommonImages.IMG_GRAY_CIRCLE);
        f_package = SLImages.getImage(CommonImages.IMG_PACKAGE);
        f_class = SLImages.getImage(CommonImages.IMG_CLASS);
        // Greenish
        f_protectedColor = new Color(display, 44, 132, 44);
        // Yellowish
        f_sharedColor = new Color(display, 255, 255, 132);
        // Reddish
        f_edtAlertColor = f_dataRaceColor = new Color(display, 255, 44, 44);
        // Gray
        f_unknownColor = new Color(display, 176, 176, 176);
        // Brownish?
        f_unsharedColor = new Color(display, 132, 88, 44);
    }

    public Image getPackage() {
        return f_package;
    }

    public Image getClazz() {
        return f_class;
    }

    public Image getConnecting() {
        return f_connecting;
    }

    public Image getNotConnected() {
        return f_notConnected;
    }

    public Image getConnected() {
        return f_connected;
    }

    public Image getError() {
        return f_error;
    }

    public Image getDone() {
        return f_done;
    }

    public Color getEdtAlertColor() {
        return f_edtAlertColor;
    }

    public Color getProtectedColor() {
        return f_protectedColor;
    }

    public Color getSharedColor() {
        return f_sharedColor;
    }

    public Color getDataRaceColor() {
        return f_dataRaceColor;
    }

    public Color getUnknownColor() {
        return f_unknownColor;
    }

    public Color getUnsharedColor() {
        return f_unsharedColor;
    }

}
