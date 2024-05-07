package com.transatron.payroll.tt;

import java.util.List;

public interface ITTService {
    void destroy();

    long getCurrentlyAvailableEnergyAmount();

    MarketInfoDAO getMarketInfo(String myAddress) throws TTServiceException;

    void newOrder(String wallet, long energy, long bandwidth, long datetime, List<String> signedTXs, String encodedPaymentTRX) throws TTServiceException;

    long estimateResourcesPriceInUSDT(String wallet, long energy, long bandwidth, long datetime) throws TTServiceException;
}
