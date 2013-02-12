package com.github.sgdesmet.androidutils.dialogs;

import android.content.DialogInterface;
import android.support.v4.app.FragmentManager;
import android.view.View;

/**
 * TODO description
 * <p/>
 * Date: 12/02/13
 * Time: 15:33
 *
 * @author: sgdesmet
 */
public interface IDefaultDialogs {
    void showProgressDialog(int resource, boolean cancelable, FragmentManager fm);

    void showProgressDialog(int resource, FragmentManager fm);

    void showErrorDialog(int message, FragmentManager fm);

    void showTerminatingErrorDialog(int message, FragmentManager fm);

    void showErrorDialog(int message, FragmentManager fm, boolean terminateApp);

    void dismiss();

    void showOneButtonDialog(int titleResource, int messageResource,
                             int buttonResourceText,
                             DialogInterface.OnClickListener buttonListener,
                             FragmentManager fm);

    void showOneButtonDialog(String titleResource, String messageResource,
                             String buttonResourceText,
                             DialogInterface.OnClickListener buttonListener,
                             FragmentManager fm);

    void showTwoButtonDialog(int titleResource, int messageResource,
                             int yesResourceText, int noResourceText,
                             DialogInterface.OnClickListener yesListener, DialogInterface.OnClickListener noListener,
                             FragmentManager fm);

    void showCustomOneButtonDialog(int titleResource, View contentView,
                                   int yesResourceText,
                                   DialogInterface.OnClickListener yesListener,
                                   FragmentManager fm);

    void showCustomTwoButtonDialog(int titleResource, View contentView,
                                   int yesResourceText, int noResourceText,
                                   DialogInterface.OnClickListener yesListener, DialogInterface.OnClickListener noListener,
                                   FragmentManager fm);
}
