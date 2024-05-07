package com.transatron.payroll.tt;

import com.transatron.payroll.IMProperty;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.proto.Response;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TTServiceWrapperClient implements ITTService {
    private static final Logger log = LogManager.getLogger(TTServiceWrapperClient.class);
    private ApiWrapper tronApiWrapper;
    private long currentlyAvailableEnergyAmount;

    private long currentlyAvailableBandwidthAmount;

    private String serverDepositAddress;

    private long currentWalletUSDTBalance;

    private int openOrderFillingPercent;

    private CloseableHttpClient httpClient = HttpClients.createDefault();

    private final String ttResourceAPIEndpoint;
    private final SimpleDateFormat jsonDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public TTServiceWrapperClient(Properties applicationSettings) {
        String apiKey = applicationSettings.getProperty(IMProperty.TRON_GRID_API_KEY);
        ttResourceAPIEndpoint = applicationSettings.getProperty(IMProperty.TRANSATRON_API_ENDPOINT);
        String dummyPK = "4601332f1b0e27bc5d382fe7d4258a16e8d073514aa61befb4f78cf9b2e33d91";
        tronApiWrapper = ApiWrapper.ofMainnet(dummyPK, apiKey);
    }

    public void destroy() {

    }

    public long getCurrentlyAvailableEnergyAmount() {
        return currentlyAvailableEnergyAmount;
    }

    public MarketInfoDAO getMarketInfo(String myAddress) throws TTServiceException {
        TTServiceException exception = null;
        String orderStatus = "NONE";
        long firstDeliveryAt = 0;
        long rentPeriod = 0;
        try {
            String requestUrl = String.format("%s/public/v1/orders?wallet_address=%s", ttResourceAPIEndpoint, myAddress);
//            String requestUrl = String.format("%s/rbundle/info", ttResourceAPIEndpoint);

//            JSONObject params = new JSONObject();
//            params.put("user_address", myAddress);

            HttpGet request = new HttpGet(requestUrl);
            Resp response = executeRPCCall(request, null);
            String jsonResponse = response.message;
            if("".equals(jsonResponse)) {
                serverDepositAddress="TPn6rAnHmgnmw8brmTw3rmBQmjKugVt5qH";
                currentlyAvailableEnergyAmount=1000000000;
                currentlyAvailableBandwidthAmount=1000000000;
                openOrderFillingPercent = -1;
            } else {
                JSONObject responseObject = new JSONObject(jsonResponse);
                if (200 == response.getCode()) {
                    serverDepositAddress = responseObject.getString("depositAddress");
                    currentlyAvailableEnergyAmount = responseObject.getLong("availableEnergy");
                    currentlyAvailableBandwidthAmount = responseObject.getLong("availableBandwidth");
                    openOrderFillingPercent = -1;
//                    currentWalletUSDTBalance = responseObject.getLong("usdt_balance");
//                    openOrderFillingPercent = responseObject.getInt("order_filling_percent");
//                    orderStatus = responseObject.getString("order_status");
//                    firstDeliveryAt = responseObject.getLong("order_first_delivery_at");
//                    rentPeriod = responseObject.getLong("order_rent_period");
                } else {
                    log.error("Error getting market info: " + jsonResponse);
                    int errorCode = responseObject.getInt("code");
                    String errorMessage = responseObject.getString("message");
                    exception = new TTServiceException(errorCode, errorMessage);
                }
            }
        } catch (Exception ex) {
            log.error("Error getting market info", ex);
            exception = new TTServiceException(600, ex.getMessage());
        }
        if (exception != null) {
            throw exception;
        } else {
            MarketInfoDAO result = new MarketInfoDAO();
            result.availableEnergy = this.currentlyAvailableEnergyAmount;
            result.availableBandwidth = this.currentlyAvailableBandwidthAmount;
            result.depositUSDTAddress = this.serverDepositAddress;
            if (openOrderFillingPercent > -1) {
                result.openOrder = new MarketInfoDAO.OrderInfoDAO();
                result.openOrder.frozen = openOrderFillingPercent;
                result.openOrder.freeze = 100;
                result.openOrder.status = orderStatus;
                result.openOrder.firstDeliveryAt = firstDeliveryAt;
                result.openOrder.rentPeriod = rentPeriod;
            }
            return result;
        }
    }

    public long estimateResourcesPriceInUSDT(String myAddress, long energy, long bandwidth, long orderdatetime) throws TTServiceException {
        Response.AccountResourceMessage accountResource = tronApiWrapper.getAccountResource(myAddress);

        long availableEnergy = accountResource.getEnergyLimit() - accountResource.getEnergyUsed();
        long availableBandwidth = accountResource.getNetLimit() - accountResource.getNetUsed();
        energy = energy > availableEnergy ? energy - availableEnergy : 0;
        bandwidth = bandwidth > availableBandwidth ? bandwidth - availableBandwidth : 0;

        long totalResourcesCostUSDT = 0;
        TTServiceException exception = null;
        try {
            String requestUrl = String.format("%s/public/v1/orders/estimate", ttResourceAPIEndpoint);

            JSONObject params = new JSONObject();
//            params.put("user_address", myAddress);
            params.put("energy", energy);
            params.put("bandwidth", bandwidth);
            params.put("fulfillFrom", jsonDateFormat.format(new Date(orderdatetime)));

            HttpPost request = new HttpPost(requestUrl);
            Resp response = executeRPCCall(request, params);
            String jsonResponse = response.message;

            JSONObject responseObject = new JSONObject(jsonResponse);
            if (200 == response.getCode()) {
                totalResourcesCostUSDT = responseObject.getLong("priceUsdt");
            } else {
                log.error("Error getting market info: " + jsonResponse);
                int errorCode = responseObject.getInt("code");
                String errorMessage = responseObject.getString("message");
                exception = new TTServiceException(errorCode, errorMessage);
            }

        } catch (Exception ex) {
            log.error("Error getting market info", ex);
            exception = new TTServiceException(600, ex.getMessage());
        }
        if (exception != null) {
            throw exception;
        } else {
            return totalResourcesCostUSDT;
        }
    }


    public void newOrder(String myAddress, long energy, long bandwidth, long orderdatetime, List<String> encodedTXs, String encodedPaymentTRX) throws TTServiceException {
        Response.AccountResourceMessage accountResource = tronApiWrapper.getAccountResource(myAddress);
        //acquire bandwidth
        long availableBandwidth = accountResource.getNetLimit() - accountResource.getNetUsed();
        long bandwidthRequired = bandwidth > availableBandwidth ? bandwidth - availableBandwidth : 0;

        long availableEnergy = accountResource.getEnergyLimit() - accountResource.getEnergyUsed();
        long energyRequired = energy > availableEnergy ? energy - availableEnergy : 0;


        TTServiceException exception = null;
        try {

            String requestUrl = String.format("%s/public/v1/orders", ttResourceAPIEndpoint);

            JSONObject params = new JSONObject();
//            params.put("user_address", myAddress);
            params.put("energy", energy);
            params.put("bandwidth", bandwidth);
            params.put("paymentTransaction", encodedPaymentTRX);
            params.put("fulfillFrom", jsonDateFormat.format(new Date(orderdatetime)));
            JSONArray txs = new JSONArray();
            for (String tx : encodedTXs) {
                txs.put(tx);
            }
            params.put("userTransactions", txs);


            HttpPost request = new HttpPost(requestUrl);
            Resp response = executeRPCCall(request, params);
            String jsonResponse = response.message;
            JSONObject responseObject = new JSONObject(jsonResponse);
            if (200 == response.getCode() || 201 == response.getCode()) {
                String orderID = responseObject.getString("orderId");
                log.info("New order ID: " + orderID);
            } else {
                log.error("Error getting market info: " + jsonResponse);
                int errorCode = responseObject.getInt("code");
                String errorMessage = responseObject.getString("message");
                exception = new TTServiceException(errorCode, errorMessage);
            }
        } catch (Exception ex) {
            log.error("Error getting market info", ex);
            exception = new TTServiceException(600, ex.getMessage());
        }

        if (exception != null) {
            throw exception;
        }
    }


    private static class Resp {
        public int code;
        public String message;

        int getCode() {
            return code;
        }
    }

    private Resp executeRPCCall(HttpUriRequestBase request, JSONObject postParams) throws IOException, ParseException {
        log.info("------------------------------------------------------------------");
        if (request instanceof HttpPost) {
            HttpPost postRequest = (HttpPost) request;
            postRequest.setHeader("Content-Type", "application/json");
            postRequest.setEntity(new StringEntity(postParams.toString()));
            log.info("POST: " + request.getPath());
            log.info("CONTENT: " + postParams.toString());
        } else {
            log.info("GET: " + request.getPath());
        }
        CloseableHttpResponse response = httpClient.execute(request);
        String jsonResponse = EntityUtils.toString(response.getEntity());
        log.info("RESPONSE: " + jsonResponse);
        log.info("------------------------------------------------------------------");
        Resp resp = new Resp();
        resp.code = response.getCode();
        resp.message = jsonResponse;
        return resp;
    }

}
