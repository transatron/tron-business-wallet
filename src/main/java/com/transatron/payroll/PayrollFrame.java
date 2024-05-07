package com.transatron.payroll;

import com.github.lgooddatepicker.components.DateTimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;
import com.transatron.payroll.tt.MarketInfoDAO;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class PayrollFrame extends JFrame implements IMPropertyListener {
    private static final Logger log = LogManager.getLogger(PayrollFrame.class);

    public enum Status {WORKING, NO_USDT, NO_RESOURCES, PREPARING_TRANSACTIONS, OBTAINING_RESOURCES, READY, DISTRIBUTING, COMPLETED}

    ;
    private JButton openFile;

    private JButton walletCopyAddress;
    private JButton walletShowQRCode;
    private JButton runActionButton;

    private JTextField marketEnergy;
    private JTextField marketBandwidth;

    private JTextField fileName;
    private JTextField fileTxNumber;
    private JTextField fileResourcesCosts;
    private JTextField fileUSDTToDistribute;
    private JTextField fileUSDTCost;
    private JTextField fileUSDTTotal;

    private JTextField walletAddress;
    private JTextField walletBalanceTRX;
    private JTextField walletBalanceUSDT;

    private JTextField walletEnergy;
    private JTextField walletBandwidth;
    private JTextField walletRemainingTime;//remaining time of resources allocation
    private DateTimePicker orderTimePicker;

    private JProgressBar distributionProgress;
    private JTextField distributionStatus;

    private JCheckBox autoDistribute;

    private NumberFormat usdtFormat = NumberFormat.getInstance();
    private NumberFormat trxFormat = NumberFormat.getInstance();
    private NumberFormat resourcesFormat = NumberFormat.getInstance();

    private Status currentStatus = Status.READY;

    private String currentErrorMessage;


    private Model model;

    public PayrollFrame(Model model) {
        this.model = model;
        init();
        model.addListener(this);
        initValues();
    }

    private void init() {
        setTitle("USDT Distributor");
        UIManager.put("TextField.disabledForeground", Color.BLACK);
        UIManager.put("TextField.disabledForeground", Color.BLACK);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        initMenu();
        usdtFormat.setMaximumFractionDigits(2);
        usdtFormat.setMinimumFractionDigits(2);
        usdtFormat.setGroupingUsed(true);
        trxFormat.setMaximumFractionDigits(0);
        trxFormat.setMinimumFractionDigits(0);
        trxFormat.setGroupingUsed(true);
        resourcesFormat.setMaximumFractionDigits(0);
        resourcesFormat.setGroupingUsed(true);
        setSize(900, 440);

        //-----------------init components-----------------
        Container root = getContentPane();
        root.setLayout(new GridBagLayout());
        //init file panel area
        marketEnergy = new JTextField();
        marketBandwidth = new JTextField();
        fileName = new JTextField();
        fileTxNumber = new JTextField();
        fileResourcesCosts = new JTextField();
        fileUSDTToDistribute = new JTextField();
        walletRemainingTime = new JTextField();
        fileUSDTCost = new JTextField();
        fileUSDTTotal = new JTextField();
        walletAddress = new JTextField();
        walletBalanceTRX = new JTextField();
        walletBalanceUSDT = new JTextField();
        walletEnergy = new JTextField();
        walletBandwidth = new JTextField();
        distributionStatus = new JTextField();

        marketEnergy.setEnabled(false);
        marketBandwidth.setEnabled(false);
        fileName.setEnabled(false);
        fileTxNumber.setEnabled(false);
        fileResourcesCosts.setEnabled(false);
        fileUSDTToDistribute.setEnabled(false);
        walletRemainingTime.setEnabled(false);
        fileUSDTCost.setEnabled(false);
        fileUSDTTotal.setEnabled(false);
        walletAddress.setEnabled(false);
        walletBalanceTRX.setEnabled(false);
        walletBalanceUSDT.setEnabled(false);
        walletEnergy.setEnabled(false);
        walletBandwidth.setEnabled(false);
        distributionStatus.setEnabled(false);

        marketEnergy.setDisabledTextColor(Color.BLACK);
        marketBandwidth.setDisabledTextColor(Color.BLACK);
        fileName.setDisabledTextColor(Color.BLACK);
        fileTxNumber.setDisabledTextColor(Color.BLACK);
        fileResourcesCosts.setDisabledTextColor(Color.BLACK);
        fileUSDTToDistribute.setDisabledTextColor(Color.BLACK);
        walletRemainingTime.setDisabledTextColor(Color.BLACK);
        fileUSDTCost.setDisabledTextColor(Color.BLACK);
        fileUSDTTotal.setDisabledTextColor(Color.BLACK);
        walletAddress.setDisabledTextColor(Color.BLACK);
        walletBalanceTRX.setDisabledTextColor(Color.BLACK);
        walletBalanceUSDT.setDisabledTextColor(Color.BLACK);
        walletEnergy.setDisabledTextColor(Color.BLACK);
        walletBandwidth.setDisabledTextColor(Color.BLACK);
        distributionStatus.setDisabledTextColor(Color.BLACK);

        autoDistribute = new JCheckBox("AutoRun Distribution");
        autoDistribute.setSelected(false);


        TimePickerSettings timeSettings = new TimePickerSettings();
        timeSettings.generatePotentialMenuTimes(TimePickerSettings.TimeIncrement.OneHour, null, null);
        orderTimePicker = new DateTimePicker(null, timeSettings);
        orderTimePicker.addDateTimeChangeListener(e -> {
            LocalDateTime localDateTime = orderTimePicker.getDateTimePermissive();
            if (localDateTime != null) {
                Date date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
                log.info("Order time: " + date);
               model.setProperty(IMProperty.ORDER_DATE_TIME, date);
            }
        });

        Calendar now = Calendar.getInstance();
        int hourOfDay = now.get(Calendar.HOUR_OF_DAY) + 1 > 23 ? 0 : now.get(Calendar.HOUR_OF_DAY) + 1;
        int dayOfMonth = hourOfDay == 0 ? now.get(Calendar.DAY_OF_MONTH) + 1 : now.get(Calendar.DAY_OF_MONTH);
        LocalDateTime localDateTime = LocalDateTime.of(now.get(Calendar.YEAR), now.get(Calendar.MONTH)+1, dayOfMonth, hourOfDay, 0, 0);
        orderTimePicker.setDateTimeStrict(localDateTime);


        //init buttons
        openFile = new JButton("Open File");
        openFile.addActionListener(e -> openFileAction());
        walletCopyAddress = new JButton("Copy");
        walletCopyAddress.addActionListener(e -> copyWalletToClipboardAction());
        walletShowQRCode = new JButton("Address QR");
        walletShowQRCode.addActionListener(e -> showQRCodeDialog());
        runActionButton = new JButton("...");
        runActionButton.addActionListener(e -> runActionButton());
        //init progress bars
        distributionProgress = new JProgressBar();
        distributionProgress.setStringPainted(true);
        distributionProgress.setMinimum(0);
        distributionProgress.setMaximum(100);
        int row = 0;
        //----- row -1
        root.add(new JLabel("Market Energy:", SwingConstants.RIGHT), new GridBagConstraints(0, row, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(marketEnergy, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
        root.add(new JLabel("Market Bandwidth:", SwingConstants.RIGHT), new GridBagConstraints(2, row, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(marketBandwidth, new GridBagConstraints(3, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        row++;
        //----- row 0
        root.add(new JLabel("File name:", SwingConstants.RIGHT), new GridBagConstraints(0, row, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(fileName, new GridBagConstraints(1, row, 3, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(openFile, new GridBagConstraints(4, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        row++;
        //----- row 1
        root.add(new JLabel("Tx Number:", SwingConstants.RIGHT), new GridBagConstraints(0, row, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(fileTxNumber, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
        root.add(new JLabel("USDT to Distribute:", SwingConstants.RIGHT), new GridBagConstraints(2, row, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(fileUSDTToDistribute, new GridBagConstraints(3, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        row++;
        //----- row 2
        root.add(new JLabel("Costs: (Energy/Net)", SwingConstants.RIGHT), new GridBagConstraints(0, row, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(fileResourcesCosts, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(new JLabel("Costs (USDT):", SwingConstants.RIGHT), new GridBagConstraints(2, row, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(fileUSDTCost, new GridBagConstraints(3, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        row++;
        //----- row 3
        root.add(new JLabel("Total (USDT):", SwingConstants.RIGHT), new GridBagConstraints(2, row, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(fileUSDTTotal, new GridBagConstraints(3, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        row++;
        //----- row 4
        root.add(new JLabel("Address:", SwingConstants.RIGHT), new GridBagConstraints(0, row, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(walletAddress, new GridBagConstraints(1, row, 3, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(walletCopyAddress, new GridBagConstraints(4, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        row++;
        //----- row 5
        root.add(new JLabel("Balance (TRX):", SwingConstants.RIGHT), new GridBagConstraints(0, row, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(walletBalanceTRX, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
        root.add(new JLabel("Balance (USDT):", SwingConstants.RIGHT), new GridBagConstraints(2, row, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(walletBalanceUSDT, new GridBagConstraints(3, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(walletShowQRCode, new GridBagConstraints(4, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        row++;
        //----- row 6
        root.add(new JLabel("Energy:", SwingConstants.RIGHT), new GridBagConstraints(0, row, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(walletEnergy, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
        root.add(new JLabel("Bandwidth:", SwingConstants.RIGHT), new GridBagConstraints(2, row, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(walletBandwidth, new GridBagConstraints(3, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        row++;
//        root.add(new JLabel("Energy:", SwingConstants.RIGHT), new GridBagConstraints(0, row, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
//        root.add(walletEnergy, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
        root.add(new JLabel("Order remaining time:", SwingConstants.RIGHT), new GridBagConstraints(2, row, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(walletRemainingTime, new GridBagConstraints(3, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        row++;
        //----- row 7
        root.add(new JLabel("Order time:", SwingConstants.RIGHT), new GridBagConstraints(0, row, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(orderTimePicker, new GridBagConstraints(1, row, 3, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(runActionButton, new GridBagConstraints(4, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        row++;
        //----- row 8
        root.add(new JLabel("Progress:", SwingConstants.RIGHT), new GridBagConstraints(0, row, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(distributionProgress, new GridBagConstraints(1, row, 3, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
//        root.add(runActionButton, new GridBagConstraints(4, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        row++;
        //----- row 9
        root.add(new JLabel("Status:", SwingConstants.RIGHT), new GridBagConstraints(0, row, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(distributionStatus, new GridBagConstraints(1, row, 3, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        root.add(autoDistribute, new GridBagConstraints(4, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
        row++;


        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                log.info("farme size" + " " + getWidth() + " " + getHeight());
            }
        });
    }

    private void initValues() {
        //init basic values
        String walletAddressValue = (String) model.getProperty(IMProperty.WALLET_ADDRESS);
        if (walletAddress != null) {
            walletAddress.setText(walletAddressValue);

        }
    }


    private void openFileAction() {
        //current folder
        File parent = new File(System.getProperty("user.dir"));
        try {
            File tmp = new File("testFile.tmp");
            parent = tmp.getCanonicalFile().getParentFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open file");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setCurrentDirectory(parent);
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".csv");
            }

            @Override
            public String getDescription() {
                return "CSV files";
            }
        });
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            model.setProperty(IMProperty.D_CSV_FILE, file);
        }
    }

    private File saveFileAction() {
        //current folder
        File parent = new File(System.getProperty("user.dir"));
        try {
            File tmp = new File("testFile.tmp");
            parent = tmp.getCanonicalFile().getParentFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save file");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setCurrentDirectory(parent);
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".csv");
            }

            @Override
            public String getDescription() {
                return "CSV files";
            }
        });
        int result = chooser.showSaveDialog(this);
        File selectedFile = null;
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
        }
        return selectedFile;
    }

    private void copyWalletToClipboardAction() {
        String walletAddressValue = (String) model.getProperty(IMProperty.WALLET_ADDRESS);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(walletAddressValue), null);
    }

    private void showQRCodeDialog(){
        BufferedImage image = (BufferedImage) model.getProperty(IMProperty.WALLET_QR_CODE);
        if(image!=null){
            QRCodeDialog dialog = new QRCodeDialog(this, image);
            dialog.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "No QR code available", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runActionButton() {
        if (currentStatus == Status.NO_USDT) {

        } else if (currentStatus == Status.NO_RESOURCES) {
            model.setProperty(IMProperty.PROGRAM_ACTION, Model.Action.GET_RESOURCES);
        } else if (currentStatus == Status.READY) {
            model.setProperty(IMProperty.PROGRAM_ACTION, Model.Action.RUN_DISTRIBUTION);
        } else if (currentStatus == Status.COMPLETED) {
            File saveToFile = saveFileAction();
            model.setProperty(IMProperty.D_EXPORT_CSV_FILE, saveToFile);
            model.setProperty(IMProperty.PROGRAM_ACTION, Model.Action.EXPORT_RESULTS);
        }
    }

    private void setStatus(Status status) {
        log.info("APP Status: " + status);
        if (status == Status.WORKING) {
            openFile.setEnabled(false);
            runActionButton.setEnabled(false);
        } else if (status == Status.NO_USDT) {
            openFile.setEnabled(true);
            runActionButton.setText("...");
            runActionButton.setEnabled(false);
        } else if (status == Status.NO_RESOURCES) {
            openFile.setEnabled(true);
            runActionButton.setText("Get Resources");
            runActionButton.setEnabled(true);
        }else if (status == Status.PREPARING_TRANSACTIONS) {
            openFile.setEnabled(true);
            runActionButton.setText("Get Resources");
            runActionButton.setEnabled(false);
        }
        else if (status == Status.OBTAINING_RESOURCES) {
            openFile.setEnabled(true);
            runActionButton.setText("Get Resources");
            runActionButton.setEnabled(false);
        } else if (status == Status.READY) {
            openFile.setEnabled(true);
            runActionButton.setText("Run Distribution");
            runActionButton.setEnabled(true);
            if(autoDistribute.isSelected()){
                log.info("Auto run distribution");
                model.setProperty(IMProperty.PROGRAM_ACTION, Model.Action.RUN_DISTRIBUTION);
            }
        } else if (status == Status.DISTRIBUTING) {
            openFile.setEnabled(true);
            runActionButton.setText("Run Distribution");
            runActionButton.setEnabled(false);
        } else if (status == Status.COMPLETED) {
            openFile.setEnabled(true);
            runActionButton.setText("Export results");
            runActionButton.setEnabled(true);
        }
        currentStatus = status;
    }


    @Override
    public void onPropertyChanged(String key, Object oldValue, Object newValue) {
        SwingUtilities.invokeLater(() -> {
            if (IMProperty.D_CSV_FILE.equals(key)) {
                String filePath = ((File) newValue).getAbsolutePath();
                fileName.setText(filePath);
            } else if (IMProperty.D_DISTRIBUTION_DATA.equals(key)) {

                DistributionData data = (DistributionData) newValue;
                long totalEnergy = data.getTotalEnergy();
                long totalBandwidth = data.getTotalBandwidth();
                double totalUSDTCost = data.getUsdtOperationCost();
                double totalUSDTToDistribute = 0;
                for (DistributionData.DistributionRow row : data.getRows()) {
                    totalUSDTToDistribute += row.amount;
                }
                double totalUSDT = totalUSDTToDistribute + totalUSDTCost;
                fileTxNumber.setText(String.valueOf(data.getRows().size()));
                fileUSDTToDistribute.setText(usdtFormat.format(totalUSDTToDistribute / Tron.USDTDecimals));
                fileResourcesCosts.setText(resourcesFormat.format(totalEnergy) + " / " + resourcesFormat.format(totalBandwidth));
                fileUSDTCost.setText(usdtFormat.format(totalUSDTCost / Tron.USDTDecimals));
                fileUSDTTotal.setText(usdtFormat.format(totalUSDT / Tron.USDTDecimals));
            } else if (IMProperty.APP_STATUS.equals(key)) {
                String statusMessage = (String) model.getProperty(IMProperty.APP_STATUS_MESSAGE);
                statusMessage = (statusMessage == null) ? "" : statusMessage;
                this.distributionStatus.setText(statusMessage);
                Integer progress = (Integer) model.getProperty(IMProperty.APP_STATUS_PROGRESS);
                if (progress == null) {
                    this.distributionProgress.setValue(0);
                } else if (progress == -1) {
                    this.distributionProgress.setIndeterminate(true);
                } else {
                    if (this.distributionProgress.isIndeterminate())
                        this.distributionProgress.setIndeterminate(false);
                    this.distributionProgress.setValue(progress);
                }
                setStatus((Status) newValue);
            } else if (IMProperty.WALLET_ADDRESS.equals(key)) {
                walletAddress.setText((String) newValue);
            } else if (IMProperty.WALLET_BALANCES.equals(key)) {
                WalletBalanceDAO balance = (WalletBalanceDAO) newValue;
                walletBalanceTRX.setText(usdtFormat.format(balance.getTrxBalance() * Tron.TRXInvDecimals));
                walletBalanceUSDT.setText(usdtFormat.format(balance.getUsdtBalance() * Tron.USDTInvDecimals));
                walletEnergy.setText(resourcesFormat.format(balance.getAvailableEnergy()));
                walletBandwidth.setText(resourcesFormat.format(balance.getAvailableBandwidth()));
            } else if (IMProperty.TT_MARKET_INFO.equals(key)) {
                MarketInfoDAO marketInfo = (MarketInfoDAO) newValue;
                marketEnergy.setText(resourcesFormat.format(marketInfo.availableEnergy));
                marketBandwidth.setText(resourcesFormat.format(marketInfo.availableBandwidth));
                if(marketInfo.openOrder != null)
                    setWalletRemainingTime(marketInfo.openOrder.firstDeliveryAt, marketInfo.openOrder.rentPeriod);
            } else if (IMProperty.PROGRAM_ERROR.equals(key)) {
                String errorMessage = (String) newValue;
                if(errorMessage.indexOf("api.node.transatron.io") >= 0){
                    errorMessage = "Error connecting to server. Please check you Internet connection!";
                }
                if(currentErrorMessage == null && errorMessage!=null) {
                    currentErrorMessage = errorMessage;
                    JOptionPane.showMessageDialog(this, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                    currentErrorMessage = null;
                }
            }
        });
    }

    private void setWalletRemainingTime(long firstDeliveryAt, long rentPeriod) {
        long currentTime = System.currentTimeMillis()/1000;
        if(firstDeliveryAt + rentPeriod > currentTime){
            long remainingTime = firstDeliveryAt + rentPeriod - currentTime;
            int seconds = (int) (remainingTime % 60);
            int minutes = (int) ((remainingTime / 60) % 60);
            int hours = (int) ((remainingTime / 3600) % 24);
            String remainingTimeStr = String.format("%02d", hours)+":"+String.format("%02d", minutes)+":"+String.format("%02d", seconds);
            walletRemainingTime.setText(remainingTimeStr);
        }
        else
            walletRemainingTime.setText("");

    }

    private void initMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem openMenuItem = new JMenuItem("Open");
        fileMenu.add(openMenuItem);

        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem exportMenuItem = new JMenuItem("Export Key");
        JMenuItem importMenuItem = new JMenuItem("Import Key");
        JMenuItem newKeyMenu = new JMenuItem("New Key");
        JMenuItem changePasswordMenuItem = new JMenuItem("Change Password");
        settingsMenu.add(exportMenuItem);
        settingsMenu.add(importMenuItem);
        settingsMenu.add(newKeyMenu);
        settingsMenu.add(changePasswordMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(settingsMenu);

        openMenuItem.addActionListener(e -> openFileAction());
        exportMenuItem.addActionListener(e -> exportKey());
        importMenuItem.addActionListener(e -> importKey());
        changePasswordMenuItem.addActionListener(e -> changePassword());
        newKeyMenu.addActionListener(e -> newKey());

        this.setJMenuBar(menuBar);
    }

    private void newKey() {
        PasswordDialog dialog = new PasswordDialog(this, model, PasswordDialog.Mode.NEW_KEY) {
            @Override
            protected void keyDecrypted(String pk) {
                JOptionPane.showMessageDialog(PayrollFrame.this, "New key generated. Please make sure you have a backup!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        };
        dialog.setVisible(true);
    }

    private void exportKey() {
        PasswordDialog dialog = new PasswordDialog(this, model, PasswordDialog.Mode.PASSWORD) {
            @Override
            protected void keyDecrypted(String pk) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(pk), null);
                JOptionPane.showMessageDialog(PayrollFrame.this, "Key copied to clipboard", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        };
        dialog.setVisible(true);
    }

    private void importKey() {
        PasswordDialog dialog = new PasswordDialog(this, model, PasswordDialog.Mode.IMPORT_KEY) {
            @Override
            protected void keyDecrypted(String pk) {
                JOptionPane.showMessageDialog(PayrollFrame.this, "Key imported successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        };
        dialog.setVisible(true);

    }

    private void changePassword() {
        PasswordDialog dialog = new PasswordDialog(this, model, PasswordDialog.Mode.CHANGE_PASSWORD) {
            @Override
            protected void keyDecrypted(String pk) {
                JOptionPane.showMessageDialog(PayrollFrame.this, "Password changed", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        };
        dialog.setVisible(true);

    }

}
