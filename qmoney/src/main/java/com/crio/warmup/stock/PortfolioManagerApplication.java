
package com.crio.warmup.stock;

import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;

import java.net.URISyntaxException;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.logging.log4j.ThreadContext;

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
    ArrayList<String> al = new ArrayList<>();
    for (int i = 0; i < symbol.length;i++) {
      al.add(symbol[i].getSymbol());
    }
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

  }
}
