package com.transatron.payroll;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.transatron.payroll.tt.ITTService;
import com.transatron.payroll.tt.MarketInfoDAO;
import com.transatron.payroll.tt.TTServiceException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;
import org.tron.trident.abi.TypeReference;
import org.tron.trident.abi.datatypes.Address;
import org.tron.trident.abi.datatypes.Bool;
import org.tron.trident.abi.datatypes.Function;
import org.tron.trident.abi.datatypes.generated.Uint256;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.contract.Trc20Contract;
import org.tron.trident.core.key.KeyPair;
import org.tron.trident.core.transaction.TransactionBuilder;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Response;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.*;

public class TronWalletController implements IMPropertyListener {

    private static final Logger log = LogManager.getLogger(TronWalletController.class);
    private Model model;
    private KeyPair wallet;
    private Trc20Contract usdtContract;

    private ApiWrapper tronApiWrapper;

    public TronWalletController(Model model) {
        this.model = model;

        model.addListener(this);
        model.scheduleRecurrent(() -> {
            refreshBalances();
        }, 5000);
        model.scheduleRecurrent(() -> {
            refreshMarketData();
        }, 7000);


    }

    private void initWithKey(String walletPK) {

        String apiKey = (String) model.getProperty(IMProperty.TRON_GRID_API_KEY);
        String grpcEndpoint = (String) model.getProperty(IMProperty.TRON_JSONRPC_ENDPOINT);
        tronApiWrapper = ApiWrapper.ofMainnet(walletPK, apiKey);
        model.setProperty(IMProperty.TRON_API_WRAPPER, tronApiWrapper);
        wallet = new KeyPair(walletPK);
        model.setProperty(IMProperty.WALLET_ADDRESS, wallet.toBase58CheckAddress());
        ThreadUtil.tronGridBouncePreventionSleep();
        org.tron.trident.core.contract.Contract contract = tronApiWrapper.getContract(Tron.USDTContractAddress);
        usdtContract = new Trc20Contract(contract, wallet.toBase58CheckAddress(), tronApiWrapper);
    }

    private void generateQRImage(String walletAddress) {
        int size = 300;
        try {
            Map<EncodeHintType, Object> hintMap = new HashMap<>();
            hintMap.put(EncodeHintType.MARGIN, 1);
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(walletAddress, BarcodeFormat.QR_CODE, size, size, hintMap);

            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            model.setProperty(IMProperty.WALLET_QR_CODE, image);
        } catch (Exception e) {
            log.error("Error while generating QR code", e);
        }
    }

    private void refreshBalances() {
        if (wallet == null) {
            return;
        }
        String apiKey = (String) model.getProperty(IMProperty.TRON_GRID_API_KEY);
        if (apiKey == null || "".equals(apiKey) || "YOUR_API_KEY_HERE".equals(apiKey) || apiKey.startsWith("YOUR")) {
            model.setProperty(IMProperty.PROGRAM_ERROR, "Please set your TronGrid API key in the settings.");
            return;
        }


        ApiWrapper apiWrapper = (ApiWrapper) model.getProperty(IMProperty.TRON_API_WRAPPER);
        Response.Account account = apiWrapper.getAccount(wallet.toBase58CheckAddress());
        ThreadUtil.tronGridBouncePreventionSleep();
        Response.AccountResourceMessage accountResource = apiWrapper.getAccountResource(wallet.toBase58CheckAddress());
        //check USDT balance
        ThreadUtil.tronGridBouncePreventionSleep();
        long usdtBalance = usdtContract.balanceOf(wallet.toBase58CheckAddress()).longValue();
        //check TRX Balance
        long trxBalance = account.getBalance();
        //check available energy and bandwidth
        long availableEnergy = accountResource.getEnergyLimit() - accountResource.getEnergyUsed();
        long availableBandwidth = accountResource.getNetLimit() - accountResource.getNetUsed();
        availableBandwidth += (accountResource.getFreeNetLimit() - accountResource.getFreeNetUsed());

        WalletBalanceDAO walletBalanceDAO = new WalletBalanceDAO(trxBalance, usdtBalance, availableEnergy, availableBandwidth);
        model.setProperty(IMProperty.WALLET_BALANCES, walletBalanceDAO);
    }

    private void refreshMarketData() {
        if (wallet == null) {
            return;
        }

        ITTService ttServiceWrapper = (ITTService) model.getProperty(IMProperty.TT_SERVICE);
        MarketInfoDAO marketInfoDAO = null;
        try {
            marketInfoDAO = ttServiceWrapper.getMarketInfo(wallet.toBase58CheckAddress());
        } catch (TTServiceException e) {
            model.setProperty(IMProperty.PROGRAM_ERROR, e.getMessage());
            return;
        }
        model.setProperty(IMProperty.TT_MARKET_INFO, marketInfoDAO);
        DistributionData distributionData = (DistributionData) model.getProperty(IMProperty.D_DISTRIBUTION_DATA);
        PayrollFrame.Status currentStatus = (PayrollFrame.Status) model.getProperty(IMProperty.APP_STATUS);
//        log.info("Refreshed market data. Current status: " + currentStatus);
        if (currentStatus == PayrollFrame.Status.OBTAINING_RESOURCES || currentStatus == PayrollFrame.Status.NO_RESOURCES) {
            {
                //re-check resources available. If still not enough, show pending...
                String errorMessage = recheckAvailableResources(distributionData);
                if (errorMessage != null) {
                    //do nothing. maybe an old order still in the database. Just wait for next refresh
                } else {
                    model.setAppStatus(PayrollFrame.Status.READY, "", 0);
                    return;
                }
            }
            if (marketInfoDAO.openOrder != null) {
                long frozen = marketInfoDAO.openOrder.frozen;
                long freeze = marketInfoDAO.openOrder.freeze;
                // log.info("Order status: " + marketInfoDAO.openOrder.status + " frozen: " + frozen + " freeze: " + freeze);
                if ("CANCELLED".equals(marketInfoDAO.openOrder.status)) {
                    model.setAppStatus(PayrollFrame.Status.NO_RESOURCES, "Order cancelled. Please try again.", 0);
                } else {
                    int percent = (int) (100f * frozen / freeze);
                    if (frozen < freeze) {
                        model.setAppStatus(PayrollFrame.Status.OBTAINING_RESOURCES, "Obtaining resources in progress...", percent);
                    }
                }
            }
        }

    }

    @Override
    public void onPropertyChanged(String key, Object oldValue, Object newValue) {
        if (IMProperty.PROGRAM_ACTION.equals(key)) {
            if (Model.Action.GET_RESOURCES.equals(newValue)) {
                getResources();
            } else if (Model.Action.RUN_DISTRIBUTION.equals(newValue)) {
                runDistribution();
            } else if (Model.Action.RE_CHECK_AVAILABLE_RESOURCES.equals(newValue)) {
                refreshMarketData();
//                String errorMessage = recheckAvailableResources((DistributionData) model.getProperty(IMProperty.D_DISTRIBUTION_DATA));
//                if (errorMessage != null) {
//                    model.setAppStatus(PayrollFrame.Status.NO_RESOURCES, "", 0);
//                } else {
//                    model.setAppStatus(PayrollFrame.Status.READY, "", 0);
//                }


            }
        } else if (IMProperty.MAIN_WALLET_PK.equals(key)) {
            initWithKey((String) newValue);
        } else if (IMProperty.WALLET_ADDRESS.equals(key)) {
            generateQRImage((String) newValue);
        }

    }

    private void getResources() {

        model.setAppStatus(PayrollFrame.Status.PREPARING_TRANSACTIONS, "Preparing transactions...", -1);
        ITTService ttServiceWrapper = (ITTService) model.getProperty(IMProperty.TT_SERVICE);
        DistributionData distributionData = (DistributionData) model.getProperty(IMProperty.D_DISTRIBUTION_DATA);
        Date orderDate = (Date) model.getProperty(IMProperty.ORDER_DATE_TIME);
        if (orderDate == null) {
            orderDate = new Date();
        }

        AccountResourcesDAO ares = getAccountResources(wallet.toBase58CheckAddress());
        long totalEnergyRequired;
        if (ares.availableEnergy > distributionData.getTotalEnergy()) {
            totalEnergyRequired = 0;
        } else {
            totalEnergyRequired = distributionData.getTotalEnergy() - ares.availableEnergy;
        }

        long totalBandwidthRequired;
        if (ares.availableBandwidth > distributionData.getTotalBandwidth()) {
            totalBandwidthRequired = 0;
        } else {
            totalBandwidthRequired = distributionData.getTotalBandwidth() - ares.availableBandwidth;
        }

        long usdtBalance = usdtContract.balanceOf(wallet.toBase58CheckAddress()).longValue();
        if (usdtBalance < distributionData.getUsdtOperationCost()) {
            model.setAppStatus(PayrollFrame.Status.NO_RESOURCES, "Error while requesting resources", 0);
            model.setProperty(IMProperty.PROGRAM_ERROR, "Not enough USDT to order resources. Please top up.");
        } else if (totalEnergyRequired > 0.8 * ttServiceWrapper.getCurrentlyAvailableEnergyAmount()) {
            model.setAppStatus(PayrollFrame.Status.NO_RESOURCES, "Error while requesting resources", 0);
            model.setProperty(IMProperty.PROGRAM_ERROR, "Not enough energy on the market. Please try again later.");
        } else {
            try {
                String encodedPaymentTx = prepareSignedOrderPayment(distributionData);
                List<String> encodedTransactions = prepareSignedTransactions(distributionData, orderDate.getTime());
                model.setAppStatus(PayrollFrame.Status.OBTAINING_RESOURCES, "Requesting resources...", -1);
                ttServiceWrapper.newOrder(wallet.toBase58CheckAddress(), totalEnergyRequired, totalBandwidthRequired, orderDate.getTime(), encodedTransactions, encodedPaymentTx);
            } catch (Exception e) {
                log.error("Error while requesting resources", e);
                model.setAppStatus(PayrollFrame.Status.NO_RESOURCES, "Error while requesting resources", 0);
                model.setProperty(IMProperty.PROGRAM_ERROR, e.getMessage());
            }
        }
    }

    private List<String> prepareSignedTransactions(DistributionData distributionData, long orderTime) {

        long orderTimePlus1Hour = orderTime + 60 * 60 * 1000;

        List<String> transactions = new ArrayList<>();
        int index = 0;
        try {
            for (DistributionData.DistributionRow row : distributionData.getRows()) {
                log.info("Signing transaction...." + (index++));
                Function transfer = new Function("transfer",
                        Arrays.asList(new Address(row.address),
                                new Uint256(BigInteger.valueOf(row.amount))),
                        Arrays.asList(new TypeReference<Bool>() {
                        }));
                long energyLimit = row.energyRequired * 420;
                ThreadUtil.tronGridBouncePreventionSleep();
                TransactionBuilder builder = tronApiWrapper.triggerCall(wallet.toBase58CheckAddress(),
                        Tron.USDTContractAddress, transfer);

                /**
                 * The fee limit refers to the upper limit of the smart contract deploy/execution cost,
                 * in TRX. Measured in SUN. The maximum limit is 1000 TRX, or 1e9 SUN.
                 */
                builder.setFeeLimit(energyLimit);
                //update expiration time
                Chain.Transaction.raw.Builder rawDataBuilder = builder.getTransaction().getRawData().toBuilder();
                rawDataBuilder.setExpiration(orderTimePlus1Hour);
                Chain.Transaction.Builder transactionBuilder = builder.getTransaction().toBuilder();
                transactionBuilder.setRawData(rawDataBuilder.build());
                builder.setTransaction(transactionBuilder.build());

                Chain.Transaction signedTxn = tronApiWrapper.signTransaction(builder.build(), wallet);
                byte[] txData = signedTxn.toByteArray();
                transactions.add(Hex.toHexString(txData));
            }
        } catch (Exception e) {
            log.error("Error while preparing transactions", e);
        }
        return transactions;
    }

    private String prepareSignedOrderPayment(DistributionData distributionData) {
        long paymentAmountUSDT = distributionData.getUsdtOperationCost();
        MarketInfoDAO marketInfoDAO = (MarketInfoDAO) model.getProperty(IMProperty.TT_MARKET_INFO);
        String depositAddress = marketInfoDAO.depositUSDTAddress;

        Function transfer = new Function("transfer",
                Arrays.asList(new Address(depositAddress),
                        new Uint256(BigInteger.valueOf(paymentAmountUSDT))),
                Arrays.asList(new TypeReference<Bool>() {
                }));

        long energyLimit = 66000 * 420;

        TransactionBuilder builder = tronApiWrapper.triggerCall(wallet.toBase58CheckAddress(),
                Tron.USDTContractAddress, transfer);
        /**
         * The fee limit refers to the upper limit of the smart contract deploy/execution cost,
         * in TRX. Measured in SUN. The maximum limit is 1000 TRX, or 1e9 SUN.
         */
        builder.setFeeLimit(energyLimit);

        //update expiration time
        Chain.Transaction.raw.Builder rawDataBuilder = builder.getTransaction().getRawData().toBuilder();
        long currentTime = System.currentTimeMillis();
        rawDataBuilder.setExpiration(currentTime + 3600 * 1000);
        Chain.Transaction.Builder transactionBuilder = builder.getTransaction().toBuilder();
        transactionBuilder.setRawData(rawDataBuilder.build());
        builder.setTransaction(transactionBuilder.build());

        Chain.Transaction signedTxn = tronApiWrapper.signTransaction(builder.build(), wallet);

        distributionData.updateUsdtOperationFundingTxHash(Tron.getTransactionHash(signedTxn));

        byte[] txData = signedTxn.toByteArray();
        String hexString = Hex.toHexString(txData);
        return hexString;
    }

    private void runDistribution() {
        model.setAppStatus(PayrollFrame.Status.READY, "Distribution will be performed on server.",-1);
//        model.setAppStatus(PayrollFrame.Status.DISTRIBUTING, "Starting distribution...", -1);
//        DistributionData distributionData = (DistributionData) model.getProperty(IMProperty.D_DISTRIBUTION_DATA);
//        //final check of resources before distribution
//        String errorMessage = recheckAvailableResources(distributionData);
//        if (errorMessage != null) {
//            model.setProperty(IMProperty.PROGRAM_ERROR, errorMessage);
//            return;
//        }
//        //double-check USDT balance
//        ThreadUtil.tronGridBouncePreventionSleep();
//        long usdtBalance = usdtContract.balanceOf(wallet.toBase58CheckAddress()).longValue();
//        long totalUSDTRequired = distributionData.getRows().stream().mapToLong(row -> row.amount).sum();
//        if (usdtBalance < totalUSDTRequired) {
//            model.setProperty(IMProperty.PROGRAM_ERROR, "Not enough USDT to distribute. Please top up.");
//            return;
//        }
//        int index = 0;
//        long totalRows = distributionData.getRows().size();
//        for (DistributionData.DistributionRow row : distributionData.getRows()) {
//            index++;
//            ThreadUtil.sleep(300);
//            if (true) {
//                row.txHash = usdtContract.transfer(row.address, row.amount, 0, "", row.energyRequired * 420);
//            }
//            log.info("Transfer to " + row.address + " amount: " + row.amount + " txHash: " + row.txHash);
//            String statusMessage = String.format("Distributing %d of %d ...", index, totalRows);
//            model.setAppStatus(PayrollFrame.Status.DISTRIBUTING, statusMessage, Math.round(100f * index / totalRows));
//
//        }
//        //verifying transactions
//        index = 0;
//        for (DistributionData.DistributionRow row : distributionData.getRows()) {
//            index++;
//            ThreadUtil.sleep(300);
//            int retry = 10;//mutliplied by 300 ms, guarantees 1 block confirmation
//            while (retry-- > 0) {
//                try {
//                    Chain.Transaction transaction = tronApiWrapper.getTransactionById(row.txHash);
//                    if (Tron.isTransactionSmartContractCallSucceeded(transaction)) {
//                        row.success = true;
//                        break;
//                    }
//                } catch (Exception e) {
//                    log.error("Error while verifying transaction " + row.txHash, e);
//                    row.success = false;
//                    ThreadUtil.sleep(300);
//                }
//            }
//            String statusMessage = String.format("Verifying transaction %d of %d ...", index, totalRows);
//            model.setAppStatus(PayrollFrame.Status.DISTRIBUTING, statusMessage, Math.round(100f * index / totalRows));
//
//        }
//
//        model.setAppStatus(PayrollFrame.Status.COMPLETED, "Distribution completed", 0);
//        model.setProperty(IMProperty.PROGRAM_ACTION, Model.Action.AUTO_EXPORT_RESULTS);
    }

    private static class AccountResourcesDAO {
        final long trxBalance;
        final long availableEnergy;
        final long availableBandwidth;

        public AccountResourcesDAO(long trxBalance, long availableEnergy, long availableBandwidth) {
            this.trxBalance = trxBalance;
            this.availableEnergy = availableEnergy;
            this.availableBandwidth = availableBandwidth;
        }
    }

    private AccountResourcesDAO getAccountResources(String walletAddress) {
        ApiWrapper apiWrapper = (ApiWrapper) model.getProperty(IMProperty.TRON_API_WRAPPER);
        Response.Account account = apiWrapper.getAccount(walletAddress);
        ThreadUtil.tronGridBouncePreventionSleep();
        Response.AccountResourceMessage accountResource = apiWrapper.getAccountResource(walletAddress);
        //check TRX Balance
        long trxBalance = account.getBalance();
        //check available energy and bandwidth
        long availableEnergy = accountResource.getEnergyLimit() - accountResource.getEnergyUsed();
        long availableBandwidth = accountResource.getNetLimit() - accountResource.getNetUsed();
        availableBandwidth += (accountResource.getFreeNetLimit() - accountResource.getFreeNetUsed());
        return new AccountResourcesDAO(trxBalance, availableEnergy, availableBandwidth);
    }

    private String recheckAvailableResources(DistributionData distributionData) {
        AccountResourcesDAO ares = getAccountResources(wallet.toBase58CheckAddress());

        String errorMessage = null;
        long bandwidthByBurningTRX = (long) (ares.trxBalance / (Tron.TRXDecimals * 0.001));

        boolean bandwidthOK = ares.availableBandwidth + bandwidthByBurningTRX >= distributionData.getTotalBandwidth();

        if (!bandwidthOK) {
            errorMessage = "Not enough bandwidth to distribute. Please top up.";
        }

        if (ares.availableEnergy < distributionData.getTotalEnergy()) {
            if (ares.availableEnergy < distributionData.getAbsoluteMinEnergy())
                errorMessage = "Not enough energy to distribute. Please top up.";
        }
        return errorMessage;
    }


}
