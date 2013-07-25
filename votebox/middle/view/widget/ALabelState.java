package votebox.middle.view.widget;

import votebox.middle.view.IFocusable;
import votebox.middle.view.IViewImage;

/**
 * @author Matt Bernhard
 * @version 0.0.1
 *          Date: 7/25/13
 */
public abstract class ALabelState {

    /**
     * This method is called by the Button when it has been asked to focus
     * itself.
     *
     * @param context
     *            This is the button that is delegating the behavior for focus.
     */
    public abstract void focus(Label context);

    /**
     * This method is called by the Button when it has been asked to unfocus
     * itself.
     *
     * @param context
     *            This is the button that is delegating the behavior for unfocus
     */
    public abstract void unfocus(Label context);

    /**
     * This method is called by the Button when it has been asked to give its
     * current image.
     *
     * @param context
     *            This is the button that is delegating the behavior for
     *            getImage.
     */
    public abstract IViewImage getImage(Label context);
}
