/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 by luxe - https://github.com/de-luxe - BURST-LUXE-RED2-G6JW-H4HG5
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package burstcoin.faucet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class BurstcoinFaucetProperties
{
  private static final Logger LOG = LoggerFactory.getLogger(BurstcoinFaucetProperties.class);
  private static final Properties PROPS = new Properties();
  static
  {
    try
    {
      PROPS.load(new FileInputStream(System.getProperty("user.dir") + "/faucet.properties"));
    }
    catch(IOException e)
    {
      LOG.error(e.getMessage());
    }
  }

  private static String walletServer;
  private static String passPhrase;
  private static String serverPort;

  public static String getServerPort()
  {
    if(serverPort == null)
    {
      serverPort = asString("burstcoin.faucet.serverPort", "8080");
    }
    return serverPort;
  }


  public static String getWalletServer()
  {
    if(walletServer == null)
    {
      walletServer = asString("burstcoin.faucet.walletServer", "http://localhost:8125");
    }
    return walletServer;
  }

  public static String getPassPhrase()
  {
    if(passPhrase == null)
    {
      passPhrase = asString("burstcoin.faucet.secretPhrase", "noPassPhrase");
      if(passPhrase.equals("noPassPhrase"))
      {
        LOG.error("Error: property 'passPhrase' is required!");
      }
    }
    return passPhrase; // we deliver "noPassPhrase", should find no plots!
  }

  private static String asString(String key, String defaultValue)
  {
    String value = PROPS.containsKey(key) ? String.valueOf(PROPS.getProperty(key)) : defaultValue;
    return StringUtils.isEmpty(value) ? defaultValue : value;
  }
}
