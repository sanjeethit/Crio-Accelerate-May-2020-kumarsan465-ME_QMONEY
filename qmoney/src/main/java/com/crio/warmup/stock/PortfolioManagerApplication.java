
package com.crio.warmup.stock;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerApplication {

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
    File file = resolveFileFromResources(args[0]);
    ObjectMapper mapper = getObjectMapper();
    PortfolioTrade[] symbol = mapper.readValue(file, PortfolioTrade[].class);
    return Arrays.asList(symbol)
      .stream().map(i -> i.getSymbol())
      .collect(Collectors.toList());
  }

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

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
    Double totalReturn = (sellPrice - buyPrice) / buyPrice;
    LocalDate purchaseDate = trade.getPurchaseDate();
    Double purYear = Double.valueOf(purchaseDate.getYear());
    Double purMonth = Double.valueOf(purchaseDate.getMonthValue());
    Double purDay = Double.valueOf(purchaseDate.getDayOfMonth());
    Double endYear = Double.valueOf(endDate.getYear());
    Double endMonth = Double.valueOf(endDate.getMonthValue());
    Double endDay = Double.valueOf(endDate.getDayOfMonth());
    Double totalYear = (endYear - purYear) + ((Math.abs(endMonth - purMonth)) / 12)
        + (Math.abs(endDay - purDay) / 365);
    Double annualizedReturn = Math.pow((1 + totalReturn),(1 / totalYear)) - 1;
    return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn);
  }

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
      throws IOException, URISyntaxException {
    List<AnnualizedReturn> annualizedReturn = new ArrayList<>();
    File resolveFile = resolveFileFromResources(args[0]);
    ObjectMapper mapper = getObjectMapper();
    PortfolioTrade[] trade = mapper.readValue(resolveFile, PortfolioTrade[].class);
    String apiToken = "186c74ef8c388ebccbeb92d69f26265a4f5eb056";
    RestTemplate restTemplate = new RestTemplate();
    for (int i = 0; i < trade.length; i++) {
      String symbol = trade[i].getSymbol();
      LocalDate startDate = trade[i].getPurchaseDate();
      LocalDate endDate = LocalDate.parse(args[1]);
      String resreadQuotes = restTemplate.getForObject(
          "https://api.tiingo.com/tiingo/daily/{symbol}/prices?startDate={sdate}&endDate={edate}&token={token}",
          String.class, symbol, startDate, endDate, apiToken);
      TiingoCandle[] tingoCandle = mapper.readValue(resreadQuotes, TiingoCandle[].class);
      annualizedReturn.add(calculateAnnualizedReturns(endDate, trade[i], tingoCandle[0].getOpen(),
           tingoCandle[tingoCandle.length - 1].getClose()));
    }
    annualizedReturn.sort((AnnualizedReturn ar1, AnnualizedReturn ar2)
        -> (ar2.getAnnualizedReturn() > ar1.getAnnualizedReturn()) 
        ? 1 : (ar2.getAnnualizedReturn() < ar1.getAnnualizedReturn()) ? -1 : 0);
    return annualizedReturn;
  }

  public static String readFileAsString(String file)
      throws JsonParseException, JsonMappingException, IOException {
    ObjectMapper mapper = getObjectMapper();
    String contents = mapper.readValue(file, String.class);
    return contents;
  }

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
    String file = args[0];
    LocalDate endDate = LocalDate.parse(args[1]);
    ObjectMapper objectMapper = getObjectMapper();
    String contents = readFileAsString(file);
    PortfolioTrade[] portfolioTrades = objectMapper.readValue(contents, PortfolioTrade[].class);
    RestTemplate restTemplate = new RestTemplate();
    return PortfolioManagerFactory.getPortfolioManager(restTemplate)
      .calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
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
    String resultOfResolveFilePathArgs0 = valueOfArgument0;
    String toStringOfObjectMapper = "ObjectMapper";
    String functionStackTrace = "mainReadFile";
    String lineStackTrace = "";
    return Arrays.asList(new String[] { valueOfArgument0, resultOfResolveFilePathArgs0,
      toStringOfObjectMapper, functionStackTrace, lineStackTrace });
  }

  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());

    printJsonObject(mainReadFile(args));
    printJsonObject(mainReadQuotes(args));
    printJsonObject(mainCalculateSingleReturn(args));
    printJsonObject(mainCalculateReturnsAfterRefactor(args));
  }
}


