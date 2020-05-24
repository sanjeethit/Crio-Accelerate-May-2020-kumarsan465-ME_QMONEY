
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
import java.util.Collections;
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

  // Read the json file provided in the argument[0]. The file will be avilable in
  // the classpath.
  // 1. Use #resolveFileFromResources to get actual file from classpath.
  // 2. parse the json file using ObjectMapper provided with #getObjectMapper,
  // and extract symbols provided in every trade.
  // return the list of all symbols in the same order as provided in json.
  // Test the function using gradle commands below
  // ./gradlew run --args="trades.json"
  // Make sure that it prints below String on the console -
  // ["AAPL","MSFT","GOOGL"]
  // Now, run
  // ./gradlew build and make sure that the build passes successfully
  // There can be few unused imports, you will need to fix them to make the build
  // pass.

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
      toStringOfObjectMapper, functionStackTrace, lineStackTrace });
  }

  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());

    printJsonObject(mainReadFile(args));
    //printJsonObject(mainReadQuotes(args));


  }
}
