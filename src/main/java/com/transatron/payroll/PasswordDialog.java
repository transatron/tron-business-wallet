package com.transatron.payroll;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;
import org.tron.trident.core.key.KeyPair;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigInteger;

public abstract class PasswordDialog extends JDialog {
    private static final Logger log = LogManager.getLogger(PasswordDialog.class);

    public enum Mode {
        PASSWORD, CHANGE_PASSWORD, IMPORT_KEY, NEW_KEY, NEW_PASSWORD
    }

    private JPasswordField passwordField;

    private JPasswordField newPasswordField;

    private JPasswordField confirmPasswordField;

    private JPasswordField newWalletPK;
    private Model model;

    private boolean passwordCorrect = false;

    private Mode currentMode = Mode.PASSWORD;


    public PasswordDialog(JFrame parent, Model model, Mode mode) {
        super(parent, "Enter Password", true);

        this.model = model;
        this.currentMode = mode;

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        int row = 0;
        passwordField = new JPasswordField(20);
        newPasswordField = new JPasswordField(20);
        confirmPasswordField = new JPasswordField(20);
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        if (mode == Mode.PASSWORD) {
            setTitle("Enter password");
            panel.add(new JLabel("Enter password:", SwingConstants.RIGHT), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
            panel.add(passwordField, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
            row++;
        } else if (mode == Mode.NEW_KEY) {
            setTitle("New key generation");
            panel.add(new JLabel("Enter password:", SwingConstants.RIGHT), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
            panel.add(passwordField, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
            row++;
        } else if (mode == Mode.CHANGE_PASSWORD) {
            setTitle("Change password");
            panel.add(new JLabel("Old password:", SwingConstants.RIGHT), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
            panel.add(passwordField, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
            row++;
            panel.add(new JLabel("New password:", SwingConstants.RIGHT), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
            panel.add(newPasswordField, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
            row++;
            panel.add(new JLabel("Confirm password:", SwingConstants.RIGHT), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
            panel.add(confirmPasswordField, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
            row++;
        } else if (mode == Mode.NEW_PASSWORD) {
            setTitle("Create password");
            panel.add(new JLabel("New password:", SwingConstants.RIGHT), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
            panel.add(newPasswordField, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
            row++;
            panel.add(new JLabel("Confirm password:", SwingConstants.RIGHT), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
            panel.add(confirmPasswordField, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
            row++;
        } else if (mode == Mode.IMPORT_KEY) {
            setTitle("Import wallet PK");
            newWalletPK = new JPasswordField(20);
            panel.add(new JLabel("Enter password:", SwingConstants.RIGHT), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
            panel.add(passwordField, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
            row++;
            panel.add(new JLabel("New wallet PK:", SwingConstants.RIGHT), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
            panel.add(newWalletPK, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
            row++;
        }

        JPanel panel1 = new JPanel();
        panel1.add(okButton);
        panel1.add(cancelButton);
        panel.add(panel1, new GridBagConstraints(0, row, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));

        passwordField.addActionListener(e -> {
            if(currentMode == Mode.PASSWORD)
                okButtonActionPerformed(e);
        });

        okButton.addActionListener(e -> okButtonActionPerformed(e));
        cancelButton.addActionListener(e -> dispose());

        getContentPane().add(panel);
        pack();
        setLocationRelativeTo(parent);
    }

    public boolean isPasswordCorrect() {
        return passwordCorrect;
    }

    private void okButtonActionPerformed(ActionEvent e) {
        if (currentMode == Mode.PASSWORD) {
            modeEnterPassword();
        } else if (currentMode == Mode.CHANGE_PASSWORD) {
            modeChangePassword();
        } else if (currentMode == Mode.IMPORT_KEY) {
            modeImportKey();
        } else if (currentMode == Mode.NEW_KEY) {
            modeNewKey();
        } else if (currentMode == Mode.NEW_PASSWORD) {
            modeNewPassword();
        }
    }

    private void modeNewKey() {
        int choice = JOptionPane.showOptionDialog(PasswordDialog.this, "New key will replace the current key. Are you sure?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, new Object[]{"No", "Yes"}, "No");
        if (choice == 1) {
            String password = new String(passwordField.getPassword());
            if (saveNewWalletPK(password, KeyPair.generate().toPrivateKey())) {
                //success callback
                keyDecrypted(null);
            }
        } else {
            dispose();
        }
    }

    private void modeNewPassword() {
        String newPassword = new String(newPasswordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(PasswordDialog.this,
                    "Passwords do not match",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (newPassword.length() < 6) {
            JOptionPane.showMessageDialog(PasswordDialog.this,
                    "Password must be at least 6 characters long",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            KeyPair keyPair = KeyPair.generate();
            String newHexEncrypted = encodeWithPassword(newPassword, "PK:" + keyPair.toPrivateKey());
            model.setProperty(IMProperty.MAIN_WALLET_PK_ECRYPTED, newHexEncrypted);
            model.setProperty(IMProperty.MAIN_WALLET_PK, keyPair.toPrivateKey());
            this.passwordCorrect = true;
            dispose();
            keyDecrypted(null);
        } catch (Exception ex) {
            log.error("Error creating new wallet with new password", ex);
            JOptionPane.showMessageDialog(PasswordDialog.this,
                    "Error saving new password",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void modeImportKey() {
        String password = new String(passwordField.getPassword());
        String newWalletKey = new String(newWalletPK.getPassword());
        if (saveNewWalletPK(password, newWalletKey)) {
            //success callback
            keyDecrypted(null);
        }
    }

    private boolean saveNewWalletPK(String password, String newWalletKey) {
        try {
            String hexEncrypted = (String) model.getProperty(IMProperty.MAIN_WALLET_PK_ECRYPTED);
            String decrypted = unpackWithPassword(password, hexEncrypted);
            if (!decrypted.startsWith("PK:")) {
                throw new Exception("Invalid password");
            }
        } catch (Exception ex) {
            log.error("Error unpacking wallet", ex);
            JOptionPane.showMessageDialog(PasswordDialog.this,
                    "Invalid password",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            KeyPair keyPair = new KeyPair(newWalletKey);
            String newPKVerified = keyPair.toPrivateKey();
            String newHexEncrypted = encodeWithPassword(password, "PK:" + newPKVerified);
            model.setProperty(IMProperty.MAIN_WALLET_PK_ECRYPTED, newHexEncrypted);
            model.setProperty(IMProperty.MAIN_WALLET_PK, newPKVerified);
            dispose();
        } catch (Exception ex) {
            log.error("Error unpacking wallet", ex);
            JOptionPane.showMessageDialog(PasswordDialog.this,
                    "Invalid private key",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        return true;
    }

    private void modeChangePassword() {
        String password = new String(passwordField.getPassword());
        String newPassword = new String(newPasswordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(PasswordDialog.this,
                    "Passwords do not match",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (newPassword.length() < 6) {
            JOptionPane.showMessageDialog(PasswordDialog.this,
                    "Password must be at least 6 characters long",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String hexEncrypted = (String) model.getProperty(IMProperty.MAIN_WALLET_PK_ECRYPTED);
            String decrypted = unpackWithPassword(password, hexEncrypted);
            String reEncrypted = encodeWithPassword(newPassword, decrypted);
            model.setProperty(IMProperty.MAIN_WALLET_PK_ECRYPTED, reEncrypted);
            dispose();
        } catch (Exception ex) {
            log.error("Error unpacking wallet", ex);
            JOptionPane.showMessageDialog(PasswordDialog.this,
                    "Invalid password",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        keyDecrypted(null);
    }

    private void modeEnterPassword() {
        String password = new String(passwordField.getPassword());
        try {
            String hexEncrypted = (String) model.getProperty(IMProperty.MAIN_WALLET_PK_ECRYPTED);
            String decrypted = unpackWithPassword(password, hexEncrypted);
            if (decrypted.startsWith("PK:")) {
                passwordCorrect = true;
                dispose();
                keyDecrypted(decrypted.substring(3));
            } else {
                throw new Exception("Invalid password");
            }
        } catch (Exception ex) {
            log.error("Error unpacking wallet", ex);
            JOptionPane.showMessageDialog(PasswordDialog.this,
                    "Invalid password",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String unpackWithPassword(String password, String hexEncrypted) throws Exception {
        BigInteger salt = new BigInteger(Model.salt, 16);
        PBEKeySpec pwSpec = new PBEKeySpec(password.toCharArray(), salt.toByteArray(), 25000, 128);
        SecretKeyFactory keyFac = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] key = keyFac.generateSecret(pwSpec).getEncoded();
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

        byte[] dataToDecrypt = Hex.decode(hexEncrypted);

        // Initialize Cipher with AES encryption mode
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        // Generate Initialization Vector (IV)
        byte[] iv2 = new byte[cipher.getBlockSize()];
        IvParameterSpec ivParameterSpecD = new IvParameterSpec(iv2);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpecD);

        byte[] decryptedBytes = cipher.doFinal(dataToDecrypt);
        String decrypted = new String(decryptedBytes);
        return decrypted;
    }

    private String encodeWithPassword(String password, String valueToEncrypt) throws Exception {
        BigInteger salt = new BigInteger(Model.salt, 16);
        PBEKeySpec pwSpec = new PBEKeySpec(password.toCharArray(), salt.toByteArray(), 25000, 128);
        SecretKeyFactory keyFac = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] key = keyFac.generateSecret(pwSpec).getEncoded();
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

        // Initialize Cipher with AES encryption mode
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        // Generate Initialization Vector (IV)
        byte[] iv2 = new byte[cipher.getBlockSize()];
        IvParameterSpec ivParameterSpecD = new IvParameterSpec(iv2);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpecD);
        byte[] encryptedBytes = cipher.doFinal(valueToEncrypt.getBytes());
        return Hex.toHexString(encryptedBytes);
    }


    protected abstract void keyDecrypted(String pk);


}
