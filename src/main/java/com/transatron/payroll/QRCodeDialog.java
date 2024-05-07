package com.transatron.payroll;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

public class QRCodeDialog extends JDialog {


    public QRCodeDialog(Frame parent, BufferedImage image) {
        super(parent, "Distributor Address", true);
        JLabel label = new JLabel(new ImageIcon(image));
        this.getContentPane().add(label);
        this.pack();
        this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        this.setLocationRelativeTo(parent);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }
}
