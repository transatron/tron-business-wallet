package com.transatron.payroll;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.ICSVParser;
import com.transatron.payroll.tt.ITTService;
import com.transatron.payroll.tt.MarketInfoDAO;
import com.transatron.payroll.tt.TTServiceException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.tron.trident.abi.TypeReference;
import org.tron.trident.abi.datatypes.Address;
import org.tron.trident.abi.datatypes.Bool;
import org.tron.trident.abi.datatypes.Function;
import org.tron.trident.abi.datatypes.generated.Uint256;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.contract.Trc20Contract;
import org.tron.trident.proto.Response;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DistributionFileController {
    private static final Logger log = LogManager.getLogger(DistributionFileController.class);
    private Model model;

    private NumberFormat usdtFormat;

    private NumberFormat parseDoubleCommaNumberFormat;

    public DistributionFileController(Model model) {
        this.model = model;
        model.addListener(this::onPropertyChanged);

        DecimalFormatSymbols customSymbols = new DecimalFormatSymbols(Locale.getDefault());
        customSymbols.setDecimalSeparator('.');
        usdtFormat = new DecimalFormat("#0.000", customSymbols);
        usdtFormat.setGroupingUsed(false);
        usdtFormat.setMaximumFractionDigits(3);
        usdtFormat.setMinimumFractionDigits(0);


        DecimalFormatSymbols customSymbols2 = new DecimalFormatSymbols(Locale.getDefault());
        customSymbols2.setDecimalSeparator(',');
        parseDoubleCommaNumberFormat = new DecimalFormat("#0.00", customSymbols2);

    }

    public ActionResult<DistributionData> runLoadFile() {
        File dataFile = (File) model.getProperty(IMProperty.D_CSV_FILE);
        if (dataFile == null) {
            return new ActionResult<>("No file selected");
        }
        model.setAppStatus(PayrollFrame.Status.WORKING, "Parsing CSV file...", -1);

        char[] separators = new char[]{',', ';', '|', '\t'};
        int separatorIndex = -1;
        DistributionData distributionData = new DistributionData();
        while(distributionData.getRows().size() == 0 && separatorIndex < separators.length) {
            separatorIndex++;
            List<String[]> csvData = null;
            MyCSVParser icsvParser = new MyCSVParser(separators[separatorIndex], '"', '\\', false, true, false, ICSVParser.DEFAULT_NULL_FIELD_INDICATOR, Locale.getDefault());
            try {
                CSVReader reader = new CSVReader(new FileReader(dataFile)) {
                    {
                        this.parser = icsvParser;
                    }
                };
                csvData = reader.readAll();
            } catch (Exception e) {
                return new ActionResult<>("Error reading file: " + e.getMessage());
            }
            if (csvData == null || csvData.isEmpty()) {
                return new ActionResult<>("No csvData found in selected file");
            }

            //parse CSV file

            for (String[] row : csvData) {
                try {
                    String address = row[0];
                    String number = row[1];
                    //dot delimited fraction separator & remove commas from numbers
                    if(number.indexOf(',') > 0 && number.indexOf('.') > 0) {
                        number = number.replaceAll(",", "");
                    }
                    //comma delimited fraction separator
                    if(number.indexOf(',') > 0) {
                        number = number.replace(',', '.');
                    }
                    Double amount = Double.parseDouble(number);

                    byte[] tronAddress = Tron.toHex(address);
                    long amountInSun = (long) (amount * Tron.TRXDecimals);
                    distributionData.addRow(Tron.toBase58(tronAddress), amountInSun);
                } catch (Exception e) {
                    //skip row if error
                }
            }
        }

        //set property so that csvData appears on the interface
        model.setProperty(IMProperty.D_DISTRIBUTION_DATA, distributionData);
        //estimate energy and bandwidth consumption
        estimateResourcesForDistribution();
        //re-set property so that csvData appears on the interface
        return new ActionResult<>(distributionData);
    }

    private void estimateResourcesForDistribution() {
        DistributionData distributionData = (DistributionData) model.getProperty(IMProperty.D_DISTRIBUTION_DATA);
        String address58from = (String) model.getProperty(IMProperty.WALLET_ADDRESS);
        ApiWrapper apiWrapper = (ApiWrapper) model.getProperty(IMProperty.TRON_API_WRAPPER);
        org.tron.trident.core.contract.Contract contract = apiWrapper.getContract(Tron.USDTContractAddress);
        Trc20Contract usdtContract = new Trc20Contract(contract, address58from, apiWrapper);
        long senderUsdtBalance = usdtContract.balanceOf(address58from).longValue();

        //we can do estimation of transactions if balance is greater than 0
        //estimate energy and bandwidth consumption
        //including 1 transaction of TT Market wallet funding
        long totalEnergyRequired = 32000;
        long totalBandwidthRequired = 345 * (distributionData.getRows().size() + 1);

        int index = 0;
        boolean preciseEstimation = true;
        for (DistributionData.DistributionRow row : distributionData.getRows()) {
            ThreadUtil.sleep(300);
            //update status
            model.setAppStatus(PayrollFrame.Status.WORKING, "Estimating resources...", Math.round(100f * (++index) / distributionData.getRows().size()));
            long energyEstimated = 66000;//default value, assuming target address USDT balance is 0
            if (row.amount <= senderUsdtBalance) {
                //estimate energy precicely
                Function transfer = new Function("transfer",
                        Arrays.asList(new Address(row.address),
                                new Uint256(BigInteger.valueOf(1))),//replaced value with 0.000001 USDT to estimate energy
                        Arrays.asList(new TypeReference<Bool>() {
                        }));
                try {
                    Response.TransactionExtention txnExtension = apiWrapper.constantCall(address58from, Tron.USDTContractAddress, transfer);
                    energyEstimated = txnExtension.getEnergyUsed();
                } catch (Exception e) {
                    log.error("Error estimating energy, probably no account for address " + address58from);
                }
            } else {
                preciseEstimation = false;
                long targetUserBalanceUSDT = usdtContract.balanceOf(row.address).longValue();
                if (targetUserBalanceUSDT == 0) {
                    energyEstimated = 66000;
                } else {
                    energyEstimated = 32000;
                }
            }
            row.bandwidthRequired = 345;
            row.energyRequired = energyEstimated;
            totalEnergyRequired += energyEstimated;
        }
        double multiplier = preciseEstimation ? 1.01 : 1.02;
        totalEnergyRequired = (long) (totalEnergyRequired * multiplier);//add some extra for safety
        distributionData.updateTotals(totalEnergyRequired, totalBandwidthRequired);
        //----------- estimate resources price in USDT
        ITTService ttServiceWrapper = (ITTService) model.getProperty(IMProperty.TT_SERVICE);
        model.setAppStatus(PayrollFrame.Status.WORKING, "Estimating resources price in USDT...", -1);
        try {
            Date orderDate = (Date)model.getProperty(IMProperty.ORDER_DATE_TIME);
            if(orderDate == null) {
                orderDate = new Date();
            }
            long usdtPrice = ttServiceWrapper.estimateResourcesPriceInUSDT(address58from, totalEnergyRequired, totalBandwidthRequired, orderDate.getTime());
            distributionData.updateOperationCost(usdtPrice);
            model.setProperty(IMProperty.D_DISTRIBUTION_DATA, distributionData);
            model.setAppStatus(PayrollFrame.Status.NO_RESOURCES, "", 0);
            model.setProperty(IMProperty.PROGRAM_ACTION, Model.Action.RE_CHECK_AVAILABLE_RESOURCES);
        } catch (TTServiceException e) {
            model.setAppStatus(PayrollFrame.Status.NO_USDT, "Error while estimating resources", 0);
            model.setProperty(IMProperty.PROGRAM_ERROR, e.getMessage());
        }

    }

    private void onPropertyChanged(String key, Object oldValue, Object newValue) {
        if (key.equals(IMProperty.D_CSV_FILE)) {
            runLoadFile();
        } else if (key.equals(IMProperty.WALLET_BALANCES)) {
            WalletBalanceDAO newWalletBalanceDAO = (WalletBalanceDAO) newValue;
            WalletBalanceDAO oldWalletBalanceDAO = (WalletBalanceDAO) oldValue;
            if (oldWalletBalanceDAO != null && oldWalletBalanceDAO.getUsdtBalance() == 0 && newWalletBalanceDAO.getUsdtBalance() > 0) {
                estimateResourcesForDistribution();
            }

        } else if (key.equals(IMProperty.PROGRAM_ACTION)) {
            if (Model.Action.EXPORT_RESULTS.equals(newValue)) {
                exportDistributionDataToCSV();
            } else if (Model.Action.AUTO_EXPORT_RESULTS.equals(newValue)) {
                autoExportToCSV();

            }
        }
    }

    private void exportDistributionDataToCSV() {
        File saveToFile = (File) model.getProperty(IMProperty.D_EXPORT_CSV_FILE);
        if (!saveToFile.getName().toLowerCase().endsWith(".csv")) {
            saveToFile = new File(saveToFile.getAbsolutePath() + ".csv");
        }

        DistributionData distributionData = (DistributionData) model.getProperty(IMProperty.D_DISTRIBUTION_DATA);
        MarketInfoDAO marketInfoDAO = (MarketInfoDAO) model.getProperty(IMProperty.TT_MARKET_INFO);

        final String[] headerRow = new String[]{"Address", "Amount", "TxHash", "Success", "Description"};
        final String[] costRow = new String[]{marketInfoDAO.depositUSDTAddress,
                usdtFormat.format(distributionData.getUsdtOperationCost() * Tron.USDTInvDecimals),
                distributionData.getUsdtOperationFundingTxHash(), "true", "Distribution cost coverage"};

        try {
            CSVWriter writer = new CSVWriter(new FileWriter(saveToFile),
                    CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);

            writer.writeNext(headerRow);
            for (DistributionData.DistributionRow row : distributionData.getRows()) {
                String[] data = new String[]{row.address, usdtFormat.format(row.amount * Tron.USDTInvDecimals), row.txHash, String.valueOf(row.success), ""};
                writer.writeNext(data);
            }
            writer.writeNext(costRow);
            writer.close();
        } catch (Exception e) {
            model.setProperty(IMProperty.PROGRAM_ERROR, e.getMessage());
            log.error("Error exporting results: ", e);
        }
    }

    private void autoExportToCSV() {
        File dataFile = (File) model.getProperty(IMProperty.D_CSV_FILE);
        String fileName = dataFile.getName();
        if (fileName.toLowerCase().endsWith(".csv")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        DistributionData distributionData = (DistributionData) model.getProperty(IMProperty.D_DISTRIBUTION_DATA);
        long usdtComission = distributionData.getUsdtOperationCost();
        String usdtCommissionString = usdtFormat.format(usdtComission * Tron.USDTInvDecimals);
        String exportFileName = fileName + "_" + simpleDateFormat.format(new Date()) + "_" + usdtCommissionString + "_USDT.csv";
        File exportFile = new File(dataFile.getParentFile(), exportFileName);
        model.setProperty(IMProperty.D_EXPORT_CSV_FILE, exportFile);
        exportDistributionDataToCSV();
        model.setAppStatus(PayrollFrame.Status.COMPLETED, "Exported to " + exportFileName, 100);
    }


}
