package com.transatron.payroll;

public class WalletBalanceDAO {
    private long trxBalance;

    private long usdtBalance;
    private long availableEnergy;
    private long availableBandwidth;

    public WalletBalanceDAO(long trxBalance, long usdtBalance, long availableEnergy,long availableBandwidth) {
        this.trxBalance = trxBalance;
        this.usdtBalance = usdtBalance;
        this.availableEnergy = availableEnergy;
        this.availableBandwidth = availableBandwidth;
    }

    public long getTrxBalance() {
        return trxBalance;
    }

    public long getAvailableEnergy() {
        return availableEnergy;
    }

    public long getAvailableBandwidth() {
        return availableBandwidth;
    }

    public long getUsdtBalance() {
        return usdtBalance;
    }
}
