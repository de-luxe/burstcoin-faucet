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

package burstcoin.faucet.network;

import burstcoin.faucet.BurstcoinFaucetProperties;
import burstcoin.faucet.network.model.Balance;
import burstcoin.faucet.network.model.SendMoneyResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class NetworkComponent
{
  private static Log LOG = LogFactory.getLog(NetworkComponent.class);
  private static final int connectionTimeout = 16000;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private HttpClient httpClient;

  public SendMoneyResponse sendMoney(int amount, String recipientId, String secretPhrase)
  {
    SendMoneyResponse sendMoneyResponse = null;
    try
    {
      ContentResponse response = httpClient.POST(BurstcoinFaucetProperties.getWalletServer() + "/burst")
        .param("requestType", "sendMoney")
        .param("recipient", recipientId)
        .param("amountNQT", amount + "00000000")
        .param("feeNQT", BurstcoinFaucetProperties.getFee()+"00000000")
        .param("deadline", "1000")
        .param("secretPhrase", secretPhrase)
        .timeout(connectionTimeout, TimeUnit.MILLISECONDS)
        .send();

      if(!response.getContentAsString().contains("errorDescription"))
      {
        sendMoneyResponse = objectMapper.readValue(response.getContentAsString(), SendMoneyResponse.class);
        LOG.info("send '" + amount + "' BURST to recipientId: '" + recipientId + "' in '" + sendMoneyResponse.getRequestProcessingTime() + "' ms");
      }
      else
      {
        LOG.error("Error: "+response.getContentAsString());
      }
    }
    catch(Exception e)
    {
      LOG.warn("Error: Failed to 'sendMoney' to accountId '" + recipientId + "' : " + e.getMessage());
    }
    return sendMoneyResponse;
  }

  public Balance getBalance(String accountId)
  {
    Balance balance = null;
    try
    {
      ContentResponse response = httpClient.POST(BurstcoinFaucetProperties.getWalletServer() + "/burst")
        .param("requestType", "getBalance")
        .param("account", accountId)
        .timeout(connectionTimeout, TimeUnit.MILLISECONDS)
        .send();

      if(!response.getContentAsString().contains("errorDescription"))
      {
        balance = objectMapper.readValue(response.getContentAsString(), Balance.class);
        LOG.info("received balance from accountId: '" + accountId + "' in '" + balance.getRequestProcessingTime() + "' ms");
      }
      else
      {
        LOG.error("Error: " + response.getContentAsString());
      }
    }
    catch(Exception e)
    {
      LOG.warn("Error: Failed to 'getBalance' for accountId '" + accountId + "' : " + e.getMessage());

    }
    return balance;
  }
}
