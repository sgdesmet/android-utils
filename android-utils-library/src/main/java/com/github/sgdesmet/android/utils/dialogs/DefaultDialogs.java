package com.github.sgdesmet.android.utils.dialogs;

import android.R;
import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.actionbarsherlock.app.SherlockDialogFragment;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import org.jetbrains.annotations.Nullable;


/**
 * TODO description
 * <p/>
 * Date: 28/09/12
 * Time: 15:20
 *
 * @author: sgdesmet
 */
public class DefaultDialogs {

    private static final String YESNO = "yesno";
    private static final String OKAY  = "okay";
    WeakReference<BaseDialogFragment> dialogFragment;

    private static final String TAG = DefaultDialogs.class.getSimpleName();


    public static interface ViewProvider extends Serializable {

        View getView();
    }


    private static class SingletonHolder {

        public static final DefaultDialogs INSTANCE = new DefaultDialogs();
    }

    public static DefaultDialogs get() {

        return SingletonHolder.INSTANCE;
    }

    protected DefaultDialogs() {

    }

    @Nullable
    protected BaseDialogFragment getDialogFragment() {

        if (dialogFragment != null)
            return dialogFragment.get();
        return null;
    }

    protected void setDialogFragment(@Nullable final BaseDialogFragment baseDialogFragment) {

        if (baseDialogFragment != null)
            dialogFragment = new WeakReference<BaseDialogFragment>( baseDialogFragment );
        else
            dialogFragment = null;
    }

    public void showProgressDialog(int resource, boolean cancelable, FragmentManager fm) {

        dismiss();
        if (fm != null) {
            BaseDialogFragment dialog = ProgressDialogFragment.newInstance( resource, cancelable );
            setDialogFragment( dialog );
            dialog.show( fm, "progress_dialog" );
        }
    }

    public void showProgressDialog(int resource, FragmentManager fm) {

        showProgressDialog( resource, false, fm );
    }

    public void showErrorDialog(int message, FragmentManager fm) {

        showErrorDialog( message, fm, false );
    }

    public void showTerminatingErrorDialog(int message, FragmentManager fm) {

        showErrorDialog( message, fm, true );
    }

    public void showErrorDialog(int message, FragmentManager fm, boolean terminateApp) {

        dismiss();
        if (fm != null) {
            BaseDialogFragment dialog = AlertDialogFragment.newInstance( message, terminateApp );
            setDialogFragment( dialog );
            dialog.show( fm, "fatal_error_dialog" );
        }
    }

    public void dismiss() {

        if (getDialogFragment() != null) {
            if (getDialogFragment().getFragmentManager() != null)
                getDialogFragment().dismissAllowingStateLoss();
            setDialogFragment( null );
        }
    }

    public void showOneButtonDialog(final Context context, final int titleResource, final int messageResource, final int buttonResourceText,
                                    final DialogInterface.OnClickListener buttonListener, FragmentManager fm) {

        dismiss();
        if (fm != null) {
            BaseDialogFragment dialog = new OneButtonDialog( context.getString( titleResource ), context.getString( messageResource ),
                    context.getString( buttonResourceText ), buttonListener );
            setDialogFragment( dialog );
            dialog.show( fm, OKAY );
        }
    }

    public void showOneButtonDialog(final String titleResource, final String messageResource, final String buttonResourceText,
                                    final DialogInterface.OnClickListener buttonListener, FragmentManager fm) {

        dismiss();
        if (fm != null) {
            BaseDialogFragment dialog = new OneButtonDialog( titleResource, messageResource, buttonResourceText, buttonListener );
            setDialogFragment( dialog );
            dialog.show( fm, OKAY );
        }
    }

    public void showTwoButtonDialog(final Context context, final int titleResource, final int messageResource, final int yesResourceText, final int noResourceText,
                                    final DialogInterface.OnClickListener yesListener, final DialogInterface.OnClickListener noListener,
                                    FragmentManager fm) {

        dismiss();
        if (fm != null) {
            BaseDialogFragment dialog = new TwoButtonDialog( context.getString( titleResource ), context.getString( messageResource ),
                    context.getString( yesResourceText ), yesListener, context.getString( noResourceText ), noListener );
            setDialogFragment( dialog );
            dialog.show( fm, YESNO );
        }
    }

    public void showTwoButtonDialog(final String title, final String message, final String yesText, final String noText,
                                    final DialogInterface.OnClickListener yesListener, final DialogInterface.OnClickListener noListener,
                                    FragmentManager fm) {

        dismiss();
        if (fm != null) {
            BaseDialogFragment dialog = new TwoButtonDialog( title, message, yesText, yesListener, noText, noListener );
            setDialogFragment( dialog );
            dialog.show( fm, YESNO );
        }
    }

    public void showCustomOneButtonDialog(final Context context, final int titleResource, final ViewProvider contentView, final int yesResourceText,
                                          final DialogInterface.OnClickListener yesListener, FragmentManager fm) {

        dismiss();
        if (fm != null) {
            BaseDialogFragment dialog = new OneButtonDialog( context.getString( titleResource ), contentView,
                    context.getString( yesResourceText ), yesListener );
            setDialogFragment( dialog );
            dialog.show( fm, YESNO );
        }
    }

    public void showCustomTwoButtonDialog(final Context context, final int titleResource, final ViewProvider contentView, final int yesResourceText,
                                          final int noResourceText, final DialogInterface.OnClickListener yesListener,
                                          final DialogInterface.OnClickListener noListener, FragmentManager fm) {

        dismiss();
        if (fm != null) {
            BaseDialogFragment dialog = new TwoButtonDialog( context.getString( titleResource ), contentView,
                    context.getString( yesResourceText ), yesListener, context.getString( noResourceText ), noListener );
            setDialogFragment( dialog );
            dialog.show( fm, YESNO );
        }
    }

    /**
     * Shows an alert dialog
     * <p/>
     * Date: 10/08/12
     * Time: 10:37
     *
     * @author: sgdesmet
     */
    public static class AlertDialogFragment extends BaseDialogFragment {

        private static final String TERMINATE = "terminate";
        private static final String MESSAGE   = "message";

        @Deprecated
        public AlertDialogFragment() {

        }

        public static AlertDialogFragment newInstance(final int messageResource, final boolean terminateApp) {

            AlertDialogFragment fragment = new AlertDialogFragment();
            Bundle arguments = new Bundle();
            arguments.putInt( MESSAGE, messageResource );
            arguments.putBoolean( TERMINATE, terminateApp );
            fragment.setArguments( arguments );
            return fragment;
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {

            setRetainInstance( true );

            final int messageResource = getArguments().getInt( MESSAGE );
            final boolean terminateApp = getArguments().getBoolean( TERMINATE );

            AlertDialog.Builder builder = new AlertDialog.Builder( getSherlockActivity() );
            builder.setCancelable( true ).setMessage( messageResource );
            builder.setNeutralButton( R.string.ok, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialogInterface, int i) {

                    DefaultDialogs.get().dismiss();
                    if (terminateApp && getSherlockActivity() != null)
                        getSherlockActivity().finish();
                }
            } );
            setCancelable( true );
            return builder.create();
        }
    }


    /**
     * Shows a loading message
     * <p/>
     * Date: 10/08/12
     * Time: 10:37
     *
     * @author: sgdesmet
     */
    public static class ProgressDialogFragment extends BaseDialogFragment {

        private static final String TITLE      = "title";
        private static final String CANCELABLE = "cancelable";

        @Deprecated
        public ProgressDialogFragment() {

        }

        public static ProgressDialogFragment newInstance(final int messageResource, final boolean terminateApp) {

            ProgressDialogFragment fragment = new ProgressDialogFragment();
            Bundle arguments = new Bundle();
            arguments.putInt( TITLE, messageResource );
            arguments.putBoolean( CANCELABLE, terminateApp );
            fragment.setArguments( arguments );
            return fragment;
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {

            int titleResource = getArguments().getInt( TITLE );
            boolean cancelable = getArguments().getBoolean( CANCELABLE );
            ProgressDialog dialog = new ProgressDialog( getSherlockActivity() );
            dialog.setMessage( getString( titleResource, true ) );
            dialog.setIndeterminate( true );
            dialog.setCancelable( cancelable );
            setCancelable( cancelable );
            return dialog;
        }
    }


    public static class OneButtonDialog extends BaseDialogFragment {

        private String                          title;
        private String                          message;
        private String                          neutralButton;
        private DialogInterface.OnClickListener neutralListener;
        private ViewProvider                    customView;
        private boolean cancelable = false;

        @Deprecated
        public OneButtonDialog() {

        }

        public OneButtonDialog(String title, String message, String neutralButton, DialogInterface.OnClickListener neutralListener) {

            this.title = title;
            this.message = message;
            this.neutralButton = neutralButton;
            this.neutralListener = neutralListener;
        }

        public OneButtonDialog(String title, ViewProvider customView, String neutralButton,
                               DialogInterface.OnClickListener neutralListener) {

            this.title = title;
            this.customView = customView;
            this.neutralButton = neutralButton;
            this.neutralListener = neutralListener;
        }

        public void onCreate(Bundle savedInstanceState) {

            super.onCreate( savedInstanceState );
            setRetainInstance( true );
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {

            if (neutralListener != null) {
                setCancelable( cancelable );
                AlertDialog.Builder builder = new AlertDialog.Builder( getSherlockActivity() );
                builder.setCancelable( cancelable )
                       .setTitle( title )
                       .setMessage( message )
                       .setNeutralButton( neutralButton, neutralListener );

                if (customView != null)
                    builder.setView( customView.getView() );

                return builder.create();
            }
            return super.onCreateDialog( savedInstanceState );
        }

        public void onDestroyView() {

            super.onDestroyView();
            if (customView != null && customView.getView() != null
                && customView.getView().getParent() != null) {
                ((ViewGroup) customView.getView().getParent()).removeView(
                        customView.getView() );
            }
        }
    }


    public static class TwoButtonDialog extends BaseDialogFragment {

        private String                          title;
        private String                          message;
        private String                          yesButton;
        private DialogInterface.OnClickListener yesListener;
        private String                          noButton;
        private DialogInterface.OnClickListener noListener;
        private ViewProvider                    customView;
        private boolean cancelable = false;

        public TwoButtonDialog() {

        }

        public TwoButtonDialog(String title, String message, String yesButton, DialogInterface.OnClickListener yesListener, String noButton,
                               DialogInterface.OnClickListener noListener) {

            this.title = title;
            this.message = message;
            this.yesButton = yesButton;
            this.yesListener = yesListener;
            this.noButton = noButton;
            this.noListener = noListener;
        }

        public TwoButtonDialog(String title, ViewProvider customView, String yesButton, DialogInterface.OnClickListener yesListener,
                               String noButton, DialogInterface.OnClickListener noListener) {

            this.title = title;
            this.customView = customView;
            this.yesButton = yesButton;
            this.yesListener = yesListener;
            this.noButton = noButton;
            this.noListener = noListener;
        }

        public void onCreate(Bundle savedInstanceState) {

            super.onCreate( savedInstanceState );
            setRetainInstance( true );
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {

            if (yesListener != null && noListener != null) {
                setCancelable( cancelable );
                AlertDialog.Builder builder = new AlertDialog.Builder( getSherlockActivity() );
                builder.setCancelable( cancelable )
                       .setTitle( title )
                       .setMessage( message )
                       .setPositiveButton( yesButton, yesListener )
                       .setNegativeButton( noButton, noListener );

                if (customView != null) {
                    builder.setView( customView.getView() );
                }

                return builder.create();
            }
            return super.onCreateDialog( savedInstanceState );
        }

        public void onDestroyView() {

            super.onDestroyView();
            if (customView != null && customView.getView() != null
                && customView.getView().getParent() != null) {
                ((ViewGroup) customView.getView().getParent()).removeView(
                        customView.getView() );
            }
        }
    }


    protected static class BaseDialogFragment extends SherlockDialogFragment {

        public BaseDialogFragment() {

        }

        public void show(FragmentManager manager, String tag) {

            try {
                //nasty hack for http://code.google.com/p/android/issues/detail?id=23096 :/
                super.show( manager, tag );
            }
            catch (IllegalStateException e) {
                Log.w( TAG, "Ignoring IllegalStateException..." );
            }
        }

        public int show(FragmentTransaction transaction, String tag) {

            try {
                //nasty hack for http://code.google.com/p/android/issues/detail?id=23096 :/
                return super.show( transaction, tag );
            }
            catch (IllegalStateException e) {
                Log.w( TAG, "Ignoring IllegalStateException..." );
                return -1;
            }
        }

        public void onDestroyView() {
            // workaround for issue http://code.google.com/p/android/issues/detail?id=17423 (dialogfragment gets dismissed
            // on orientation change)
            if (getDialog() != null && getRetainInstance())
                getDialog().setDismissMessage( null );
            super.onDestroyView();
        }
    }
}
