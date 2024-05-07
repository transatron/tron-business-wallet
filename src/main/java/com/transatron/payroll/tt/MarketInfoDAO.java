package com.transatron.payroll.tt;

public class MarketInfoDAO {

    public static class OrderInfoDAO{
        public long orderID;
        public String status;
        public long freeze;
        public long frozen;
        public long firstDeliveryAt;
        public long rentPeriod;
    }

    public long availableEnergy;
    public long availableBandwidth;
    public OrderInfoDAO openOrder;
    public String depositUSDTAddress;
}
