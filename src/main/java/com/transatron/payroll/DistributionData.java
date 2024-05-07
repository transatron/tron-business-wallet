package com.transatron.payroll;

import java.util.ArrayList;
import java.util.List;

public class DistributionData {

    private long totalEnergy;
    private long totalBandwidth;
    private long usdtOperationCost;
    private List<DistributionRow> rows;

    private String usdtOperationFundingTxHash;

    public static class DistributionRow{
        final String address;
        final long amount;
        String txHash;
        long bandwidthRequired;
        long energyRequired;

        boolean success;
        public DistributionRow(String address, long amount) {
            this.address = address;
            this.amount = amount;
        }
    }

    public DistributionData(){
        rows = new ArrayList<>();
    }

    public DistributionData(List<DistributionRow> rows) {
        this.rows = rows;
    }

    public List<DistributionRow> getRows() {
        return rows;
    }

    void updateTotals(long totalEnergy, long totalBandwidth){
        this.totalEnergy = totalEnergy;
        this.totalBandwidth = totalBandwidth;
    }

    void updateOperationCost(long usdtOperationCost){
        this.usdtOperationCost = usdtOperationCost;
    }

    void updateUsdtOperationFundingTxHash(String usdtOperationFundingTxHash) {
        this.usdtOperationFundingTxHash = usdtOperationFundingTxHash;
    }


    public void addRow(String address, long amount){
        DistributionRow row = new DistributionRow(address, amount);
        rows.add(row);
    }

    public long getTotalEnergy() {
        return totalEnergy;
    }

    public long getTotalBandwidth() {
        return totalBandwidth;
    }

    public long getUsdtOperationCost() {
        return usdtOperationCost;
    }

    public long getAbsoluteMinEnergy(){
        return rows.stream().mapToLong(row -> row.energyRequired).sum();
    }

    public String getUsdtOperationFundingTxHash() {
        return usdtOperationFundingTxHash;
    }
}
