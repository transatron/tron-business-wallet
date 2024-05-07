# Token Distributor Application
Designed to distribute TRC20 tokens to multiple addresses from CSV file. 
This app does accurate estimation of Energy and Bandwidth required to distribute tokens, then automatically acquires resources via Transatron service. All transaction costs are covered via separate USDT transaction.

# Prerequisites

In order to build this service you have to have an installed:

* [Maven](https://maven.apache.org/download.cgi)
* [JDK 8](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html#A1097257)

# Building and running

1. Clone the repository

2. Run build with: ```mvn clean install```

3. Create settings file from template: ```cp ./settings_template.xml ./settings.xml```

4. Create your own Tron Grid API Key and replace `YOUR_API_KEY` in `settings.xml`

5. Run app: ```java -jar target/TronBusinessWallet-1.0-jar-with-dependencies.jar```
