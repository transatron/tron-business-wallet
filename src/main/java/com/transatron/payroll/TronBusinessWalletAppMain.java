package com.transatron.payroll;

import com.transatron.payroll.tt.TTServiceWrapperClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class TronBusinessWalletAppMain {
    private static final Logger log = LogManager.getLogger(TronBusinessWalletAppMain.class);

    private static final String settingsFile = "settings.xml";

    public static void main(String[] args) {
        log.info("Starting TronBusinessWallet Application v2.0.0");
        Properties applicationSettings = new Properties();
        try {
            applicationSettings.loadFromXML(new FileInputStream(settingsFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Model model = new Model(applicationSettings);
        model.addListener((key, oldValue, newValue) -> {
            if(IMProperty.MAIN_WALLET_PK_ECRYPTED.equals(key)){
                savePropertiesToFile(model);
            }
        });
//        TTServiceWrapper ttServiceWrapper = new TTServiceWrapper(applicationSettings);
        TTServiceWrapperClient ttServiceWrapper = new TTServiceWrapperClient(applicationSettings);
        model.setProperty(IMProperty.TT_SERVICE, ttServiceWrapper);
        //-----------------------
        TronWalletController walletController = new TronWalletController(model);
        DistributionFileController fileController = new DistributionFileController(model);
        PayrollFrame frame = new PayrollFrame(model);
        //-----------------------
        // Get the screen dimensions
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        // Calculate the center coordinates for the JFrame
        int x = (screenSize.width - frame.getWidth()) / 2;
        int y = (screenSize.height - frame.getHeight()) / 2;

        // Set the location of the JFrame to the center coordinates
        frame.setLocation(x, y);
        //-----------------------

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                PayrollFrame.Status appStatus = (PayrollFrame.Status) model.getProperty(IMProperty.APP_STATUS);
                if(appStatus != PayrollFrame.Status.DISTRIBUTING){
                    int choice = JOptionPane.showOptionDialog(frame, "Are you sure you want to close the application?", "Close Application", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"No", "Yes"}, "No");
                    if(choice == 1) {
                        frame.dispose();
                        ttServiceWrapper.destroy();
                        model.destroy();
                        System.exit(0);
                    }
                } else {
                    model.setProperty(IMProperty.PROGRAM_ERROR, "Cannot close application while performing distribution.");
                }
            }
        });

        frame.setVisible(true);

        Object encryptedWallet = model.getProperty(IMProperty.MAIN_WALLET_PK_ECRYPTED);
        if(encryptedWallet == null || encryptedWallet.toString().isEmpty()){
            showNewPasswordDialog(frame, model, ttServiceWrapper);
        } else {
            showPasswordDialog(frame, model, ttServiceWrapper);
        }


    }

    private static void showNewPasswordDialog(PayrollFrame frame, Model model, TTServiceWrapperClient ttServiceWrapper) {
        PasswordDialog dialog = new PasswordDialog(frame, model, PasswordDialog.Mode.NEW_PASSWORD) {
            @Override
            protected void keyDecrypted(String pk) {

            }
        };
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            //closing with "close" button on window
            @Override
            public void windowClosing(WindowEvent windowEvent) {

                if (!dialog.isPasswordCorrect()) {
                    ttServiceWrapper.destroy();
                    model.destroy();
                    System.exit(0);
                }
            }

            //closing by OK/Cancel buttons
            @Override
            public void windowClosed(WindowEvent e) {
                if (!dialog.isPasswordCorrect()) {
                    ttServiceWrapper.destroy();
                    model.destroy();
                    System.exit(0);
                }
            }
        });
        dialog.setVisible(true);
    }

    private static void showPasswordDialog(PayrollFrame frame, Model model, TTServiceWrapperClient ttServiceWrapper) {
        PasswordDialog dialog = new PasswordDialog(frame, model, PasswordDialog.Mode.PASSWORD) {
            @Override
            protected void keyDecrypted(String pk) {
                model.setProperty(IMProperty.MAIN_WALLET_PK, pk);
            }
        };
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            //closing with "close" button on window
            @Override
            public void windowClosing(WindowEvent windowEvent) {

                if (!dialog.isPasswordCorrect()) {
                    ttServiceWrapper.destroy();
                    model.destroy();
                    System.exit(0);
                }
            }

            //closing by OK/Cancel buttons
            @Override
            public void windowClosed(WindowEvent e) {
                if (!dialog.isPasswordCorrect()) {
                    ttServiceWrapper.destroy();
                    model.destroy();
                    System.exit(0);
                }
            }
        });
        dialog.setVisible(true);
    }

    private static void savePropertiesToFile(Model model) {
        Properties applicationSettings = new Properties();
        applicationSettings.setProperty(IMProperty.MAIN_WALLET_PK_ECRYPTED, model.getProperty(IMProperty.MAIN_WALLET_PK_ECRYPTED).toString());
        applicationSettings.setProperty(IMProperty.TRON_GRID_API_KEY, model.getProperty(IMProperty.TRON_GRID_API_KEY).toString());
        applicationSettings.setProperty(IMProperty.TRON_JSONRPC_ENDPOINT, model.getProperty(IMProperty.TRON_JSONRPC_ENDPOINT).toString());
        applicationSettings.setProperty(IMProperty.TRANSATRON_API_ENDPOINT, model.getProperty(IMProperty.TRANSATRON_API_ENDPOINT).toString());

        try {
            applicationSettings.storeToXML(new FileOutputStream(settingsFile), "Distributor Application Settings");
        } catch (IOException e) {
            log.error("Error saving application settings", e);
            model.setProperty(IMProperty.PROGRAM_ERROR, "Error saving application settings: "+e.getMessage());
        }
    }


}
