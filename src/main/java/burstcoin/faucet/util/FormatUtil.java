package burstcoin.faucet.util;

import burstcoin.faucet.BurstcoinFaucetProperties;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class FormatUtil
{
  private static final BigDecimal DECIMAL_DIVISOR = new BigDecimal(100000000L);

  public static String formatAmount(Long amount, NumberFormat format)
  {
    double value = new BigDecimal(amount).divide(DECIMAL_DIVISOR, BurstcoinFaucetProperties.getDigits(), BigDecimal.ROUND_UP).doubleValue();
    return format.format(value);
  }

  public static NumberFormat getFormatter(Locale locale)
  {
    NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
    numberFormat.setMaximumFractionDigits(BurstcoinFaucetProperties.getDigits());
    numberFormat.setMinimumFractionDigits(0);
    return numberFormat;
  }
}
