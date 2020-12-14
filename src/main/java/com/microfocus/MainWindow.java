package com.microfocus;

import com.hpe.adm.nga.sdk.Octane;
import com.hpe.adm.octane.ideplugins.services.connection.ConnectionSettings;
import com.hpe.adm.octane.ideplugins.services.connection.granttoken.GrantTokenAuthentication;
import com.hpe.adm.octane.ideplugins.services.connection.granttoken.TokenPollingCompleteHandler;
import com.hpe.adm.octane.ideplugins.services.connection.granttoken.TokenPollingInProgressHandler;
import com.hpe.adm.octane.ideplugins.services.connection.granttoken.TokenPollingStartedHandler;
import com.hpe.adm.octane.ideplugins.services.exception.ServiceException;
import com.hpe.adm.octane.ideplugins.services.util.UrlParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class MainWindow {

    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);
    protected Shell shlOctaneSsoDebug;
    private Text textUrl;
    private Text textConsole;
    private Label labelSharedspaceValue;
    private Label lblWorkspaceValue;
    private Label lblResult;
    private ConnectionSettings connectionSettings;
    private Button btnLogin;
    private Thread thread;
    private TokenPollingStartedHandler tokenPollingStartedHandler;
    private TokenPollingInProgressHandler tokenPollingInProgressHandler;
    private TokenPollingCompleteHandler tokenPollingCompleteHandler;
    private Link lblLoginUrlValue;
    private final static String loginUrlPlaceholder = "<populated after Login is pressed>";
    private String loginUrl = "";

    public static void main(String[] args) {
        try {
            MainWindow window = new MainWindow();
            window.open();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public void open() {
        Display display = Display.getDefault();
        createContents();
        shlOctaneSsoDebug.open();
        shlOctaneSsoDebug.layout();
        while (!shlOctaneSsoDebug.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        if (thread != null) {
            thread.stop();
        }
    }

    /**
     * Create contents of the window.
     */
    protected void createContents() {
        shlOctaneSsoDebug = new Shell();
        shlOctaneSsoDebug.setSize(894, 683);
        shlOctaneSsoDebug.setText("Octane SSO Debug");
        shlOctaneSsoDebug.setLayout(new GridLayout(2, false));

        Label lblOctaneUrl = new Label(shlOctaneSsoDebug, SWT.NONE);
        lblOctaneUrl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblOctaneUrl.setText("Octane URL:");

        textUrl = new Text(shlOctaneSsoDebug, SWT.BORDER);
        textUrl.addModifyListener(e -> {
            try {
                connectionSettings = UrlParser.resolveConnectionSettings(textUrl.getText(), new GrantTokenAuthentication());
                labelSharedspaceValue.setText(connectionSettings.getSharedSpaceId() + "");
                lblWorkspaceValue.setText(connectionSettings.getWorkspaceId() + "");
                btnLogin.setEnabled(true);
            } catch (ServiceException e1) {
                labelSharedspaceValue.setText("");
                lblWorkspaceValue.setText("");
                lblResult.setText(e1.getMessage());
                btnLogin.setEnabled(false);
            }
        });
        textUrl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblSharedspace = new Label(shlOctaneSsoDebug, SWT.NONE);
        lblSharedspace.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblSharedspace.setText("Sharedspace:");

        labelSharedspaceValue = new Label(shlOctaneSsoDebug, SWT.NONE);
        labelSharedspaceValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

        Label lblWorkspace = new Label(shlOctaneSsoDebug, SWT.NONE);
        lblWorkspace.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblWorkspace.setText("Workspace:");

        lblWorkspaceValue = new Label(shlOctaneSsoDebug, SWT.NONE);
        lblWorkspaceValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        new Label(shlOctaneSsoDebug, SWT.NONE);

        btnLogin = new Button(shlOctaneSsoDebug, SWT.NONE);

        tokenPollingStartedHandler = loginUrl -> {
            logger.debug("Login URL: " + loginUrl);
            Display.getDefault().asyncExec(() -> {
                lblLoginUrlValue.setText("<a href=\"" + loginUrl + "\">" + loginUrl + "</a>");
                MainWindow.this.loginUrl = loginUrl;
            });
        };

        tokenPollingInProgressHandler = pollingStatus -> {
            logger.debug("polling time left: " + (pollingStatus.timeoutTimeStamp - new Date().getTime()) / 1000);
            return pollingStatus;
        };

        tokenPollingCompleteHandler = tokenPollingCompletedStatus -> logger.debug(tokenPollingCompletedStatus.toString());

        btnLogin.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                btnLogin.setEnabled(false);
                if (thread != null) {
                    textConsole.setText("");
                    thread.stop();
                }

                thread = new Thread(() -> {
                    try {
                        Octane octane = OctaneService.doLogin(connectionSettings, tokenPollingStartedHandler, tokenPollingInProgressHandler, tokenPollingCompleteHandler);
                        logger.info("Octane defects count: " +
                                octane.entityList("defects")
                                        .get()
										.addFields("id")
                                        .execute()
                                        .getTotalCount()
                        );
                    } catch (Exception ex) {
                        String stacktrace = ExceptionUtils.getStackTrace(ex);
                        logger.error(stacktrace);
                    }
                    Display.getDefault().asyncExec(() -> btnLogin.setEnabled(true));
                });
                thread.start();
            }
        });

        btnLogin.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        btnLogin.setText("Login");
        btnLogin.setEnabled(false);
        new Label(shlOctaneSsoDebug, SWT.NONE);

        lblResult = new Label(shlOctaneSsoDebug, SWT.NONE);
        lblResult.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

        Label lblLoginUrl = new Label(shlOctaneSsoDebug, SWT.NONE);
        lblLoginUrl.setText("Login URL:");

        lblLoginUrlValue = new Link(shlOctaneSsoDebug, SWT.NONE);
        lblLoginUrlValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        lblLoginUrlValue.setText(loginUrlPlaceholder);
        lblLoginUrlValue.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Program.launch(loginUrl);
            }
        });

        Label label = new Label(shlOctaneSsoDebug, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

        ScrolledComposite scrolledComposite = new ScrolledComposite(shlOctaneSsoDebug, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        GridData gd_scrolledComposite = new GridData(SWT.FILL, SWT.FILL, false, true, 2, 1);
        gd_scrolledComposite.heightHint = 96;
        gd_scrolledComposite.widthHint = 430;
        scrolledComposite.setLayoutData(gd_scrolledComposite);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);

        textConsole = new Text(scrolledComposite, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL);
        textConsole.setEditable(false);
        scrolledComposite.setContent(textConsole);
        scrolledComposite.setMinSize(textConsole.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        TextAreaAppender.textArea = textConsole;
        new Label(shlOctaneSsoDebug, SWT.NONE);

        Button btnSaveToFile = new Button(shlOctaneSsoDebug, SWT.NONE);
        btnSaveToFile.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        btnSaveToFile.setText("Save To File");

		btnSaveToFile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fileSave = new FileDialog(btnSaveToFile.getShell(), SWT.SAVE);
				fileSave.setFilterNames(new String[] { "log" });
				fileSave.setFilterExtensions(new String[] { "*.log" });
				fileSave.setFilterPath("C:\\"); // Windows path
				fileSave.setFileName("octane_sso_"+ new Date().getTime() +".log");
                fileSave.setOverwrite(false);
				String open = fileSave.open();
				if(open != null) {
                    File file = new File(open);
                    try {
                        boolean created = file.createNewFile();
                        if(created) {
                            FileUtils.writeStringToFile(file, textConsole.getText());
                            logger.info("Log Saved as: " + file.getCanonicalPath());
                        } else {
                            logger.error("Cannot create log file: " + open);
                        }
                    } catch (IOException e1) {
                        logger.error(e1.toString());
                    }
                }
			}
		});
    }

}
