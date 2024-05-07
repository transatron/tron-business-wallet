package com.transatron.payroll.tt;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

public class CoinMarketCapPriceChecker {
    private static final Logger log = LogManager.getLogger(CoinMarketCapPriceChecker.class);
    private String cmcAPIKey;

    private Date lastCheckedAt;

    private double lastPrice = 0.1;

    public CoinMarketCapPriceChecker(String cmcAPIKey) {
        this.cmcAPIKey = cmcAPIKey;
    }

    public double getTrxPrice() {
        final String symbol = "TRX";

        if(lastCheckedAt!=null && (new Date().getTime() - lastCheckedAt.getTime()) < 1000*5*60) {
            return lastPrice;
        }

        try {
            log.info("Reading price from CMC...");
            // Create URL with parameters
            String urlString = "https://pro-api.coinmarketcap.com/v2/cryptocurrency/quotes/latest?symbol=" + symbol;
            URL url = new URL(urlString);

            // Open connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-CMC_PRO_API_KEY", cmcAPIKey);
            connection.setRequestProperty("Accept", "application/json");

            // Read response
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            lastPrice = extractPriceFromResponse(response.toString());
            lastCheckedAt = new Date();

            // Close connection
            connection.disconnect();
        } catch (IOException e) {
            log.error("Error checking price of " + symbol, e);
        }

        return lastPrice;
    }

    private static double extractPriceFromResponse(String jsonResponse) {
        JSONObject responseJson = new JSONObject(jsonResponse);
        JSONArray trxArray = responseJson.getJSONObject("data").getJSONArray("TRX");
        JSONObject trxJson = trxArray.getJSONObject(0);
        JSONObject quoteJson = trxJson.getJSONObject("quote");
        JSONObject usdJson = quoteJson.getJSONObject("USD");
        double price = usdJson.getDouble("price");
        return price;
    }
}
