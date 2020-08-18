
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {




  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  // TODO: CRIO_TASK_MODULE_REFACTOR
  // Now we want to convert our code into a module, so we will not call it from
  // main anymore.
  // Copy your code from Module#3
  // PortfolioManagerApplication#calculateAnnualizedReturn
  // into #calculateAnnualizedReturn function here and make sure that it
  // follows the method signature.
  // Logic to read Json file and convert them into Objects will not be required
  // further as our
  // clients will take care of it, going forward.
  // Test your code using Junits provided.
  // Make sure that all of the tests inside PortfolioManagerTest using command
  // below -
  // ./gradlew test --tests PortfolioManagerTest
  // This will guard you against any regressions.
  // run ./gradlew build in order to test yout code, and make sure that
  // the tests and static code quality pass.

  // CHECKSTYLE:OFF

  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  // CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  // Extract the logic to call Tiingo thirdparty APIs to a separate function.
  // It should be split into fto parts.
  // Part#1 - Prepare the Url to call Tiingo based on a template constant,
  // by replacing the placeholders.
  // Constant should look like
  // https://api.tiingo.com/tiingo/daily/<ticker>/prices?startDate=?&endDate=?&token=?
  // Where ? are replaced with something similar to <ticker> and then actual url
  // produced by
  // replacing the placeholders with actual parameters.

  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    String resreadQuotes = restTemplate.getForObject(buildUri(symbol, from, to), String.class);
    Candle[] tingoCandles = mapper.readValue(resreadQuotes, TiingoCandle[].class);
    return Arrays.asList(tingoCandles);
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String apiKey = "186c74ef8c388ebccbeb92d69f26265a4f5eb056";
    String uriTemplate = "https://api.tiingo.com/tiingo/daily/%1$s/prices?startDate=%2$s&endDate=%3$s&token=%4$s";
    String formatedUriTemplate = String.format(uriTemplate, symbol, startDate, endDate, apiKey);
    return formatedUriTemplate;

  }

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade, Double buyPrice,
      Double sellPrice) {
    Double totalReturn = (sellPrice - buyPrice) / buyPrice;
    LocalDate purchaseDate = trade.getPurchaseDate();
    Double purYear = Double.valueOf(purchaseDate.getYear());
    Double purMonth = Double.valueOf(purchaseDate.getMonthValue());
    Double purDay = Double.valueOf(purchaseDate.getDayOfMonth());
    Double endYear = Double.valueOf(endDate.getYear());
    Double endMonth = Double.valueOf(endDate.getMonthValue());
    Double endDay = Double.valueOf(endDate.getDayOfMonth());
    Double totalYear = (endYear - purYear) + ((Math.abs(endMonth - purMonth)) / 12) + (Math.abs(endDay - purDay) / 365);
    Double annualizedReturn = Math.pow((1 + totalReturn), (1 / totalYear)) - 1;
    return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn);
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades, LocalDate endDate)
      throws JsonProcessingException {
    List<AnnualizedReturn> annualizedReturn = new ArrayList<>();

    for (int i = 0; i < portfolioTrades.size(); i++) {
      String symbol = portfolioTrades.get(i).getSymbol();
      LocalDate from = portfolioTrades.get(i).getPurchaseDate();
      List<Candle> tingoCandles = getStockQuote(symbol, from, endDate);
      Double buyPrice = tingoCandles.get(0).getOpen();
      Double sellPrice = tingoCandles.get(tingoCandles.size() - 1).getClose();
      annualizedReturn.add(calculateAnnualizedReturns(endDate, portfolioTrades.get(i), buyPrice, sellPrice));
    }
    annualizedReturn.sort(getComparator());
    return annualizedReturn;
  }
}
