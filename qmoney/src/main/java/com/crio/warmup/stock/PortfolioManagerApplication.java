
package com.crio.warmup.stock;

import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;

import java.net.URISyntaxException;
import java.nio.file.Paths;

import java.time.LocalDate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerApplication {

  // Copy the relavent code from #mainReadFile to parse the Json into
  // PortfolioTrade list.
  // Now That you have the list of PortfolioTrade already populated in module#1
  // For each stock symbol in the portfolio trades,
  // Call Tiingo api
  // (https://api.tiingo.com/tiingo/daily/<ticker>/prices?startDate=&endDate=&token=)
  // with
  // 1. ticker = symbol in portfolio_trade
  // 2. startDate = purchaseDate in portfolio_trade.
  // 3. endDate = args[1]
  // Use RestTemplate#getForObject in order to call the API,
  // and deserialize the results in List<Candle>
  // Note - You may have to register on Tiingo to get the api_token.
  // Please refer the the module documentation for the steps.
  // Find out the closing price of the stock on the end_date and
  // return the list of all symbols in ascending order by its close value on
  // endDate
  // Test the function using gradle commands below
  // ./gradlew run --args="trades.json 2020-01-01"
  // ./gradlew run --args="trades.json 2019-07-01"
  // ./gradlew run --args="trades.json 2019-12-03"
  // And make sure that its printing correct results.
  public static List<String> mainReadQuotes(String[] args) throws IOException,
      URISyntaxException, RestClientException {
    File resolveFile = resolveFileFromResources(args[0]);
    ObjectMapper mapper = getObjectMapper();
    PortfolioTrade[] res = mapper.readValue(resolveFile, PortfolioTrade[].class);
    String apiToken = "186c74ef8c388ebccbeb92d69f26265a4f5eb056";
    RestTemplate restTemplate = new RestTemplate();
    SortedMap<Double, String> smap = new TreeMap<>();
    for (int i = 0; i < res.length; i++) {
      String symbol = res[i].getSymbol();
      LocalDate startDate = res[i].getPurchaseDate();
      LocalDate endDate = LocalDate.parse(args[1]);
      String resreadQuotes = restTemplate.getForObject(
          "https://api.tiingo.com/tiingo/daily/{symbol}/prices?startDate={sdate}&endDate={edate}&token={token}",
          String.class, symbol, startDate, endDate, apiToken);
      TiingoCandle[] readQuotes = mapper.readValue(resreadQuotes, TiingoCandle[].class);
      smap.put(readQuotes[readQuotes.length - 1].getClose(), res[i].getSymbol());
    }
    List<String> al = new ArrayList<>(smap.values());
    return al;
  }

  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return (Paths.get(Thread.currentThread().getContextClassLoader()
    .getResource(filename).toURI()).toFile());
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  public static List<String> debugOutputs() {
    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = "";
    String toStringOfObjectMapper = "";
    String functionStackTrace = "";
    String lineStackTrace = "";
    return Arrays.asList(new String[] { valueOfArgument0, resultOfResolveFilePathArgs0,
      toStringOfObjectMapper,functionStackTrace,lineStackTrace });
  }

  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());
    printJsonObject(mainReadQuotes(args));
  }
}
