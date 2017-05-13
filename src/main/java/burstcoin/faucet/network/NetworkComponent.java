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
import burstcoin.faucet.network.model.MiningInfo;
import burstcoin.faucet.network.model.SendMoneyResponse;
import burstcoin.faucet.network.model.Timestamp;
import burstcoin.faucet.network.model.Transaction;
import burstcoin.faucet.network.model.Transactions;
import com.fasterxml.jackson.databind.ObjectMapper;
import nxt.crypto.ReedSolomon;
import nxt.util.Convert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class NetworkComponent
{
  private static final String FAUCET_ACCOUNT_RS = "BURST-" + ReedSolomon
    .encode(Convert.parseUnsignedLong(BurstcoinFaucetProperties.getNumericFaucetAccountId()));
  private static final String BURST_API_URL = BurstcoinFaucetProperties.getWalletServer() + "/burst";
  private static Log LOG = LogFactory.getLog(NetworkComponent.class);

  private ObjectMapper objectMapper;
  private HttpClient httpClient;

  @Autowired
  public NetworkComponent(HttpClient httpClient, ObjectMapper objectMapper)
  {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  public SendMoneyResponse sendMoney(int amount, String recipientId, String secretPhrase)
  {
    SendMoneyResponse sendMoneyResponse = null;
    try
    {
      ContentResponse response = httpClient.POST(BurstcoinFaucetProperties.getWalletServer() + "/burst")
        .param("requestType", "sendMoney")
        .param("recipient", recipientId)
        .param("amountNQT", amount + "00000000")
        .param("feeNQT", BurstcoinFaucetProperties.getFee() + "00000000")
        .param("deadline", "1000")
        .param("secretPhrase", secretPhrase)
        .timeout(BurstcoinFaucetProperties.getConnectionTimeout(), TimeUnit.MILLISECONDS)
        .send();

      if(!response.getContentAsString().contains("errorDescription"))
      {
        sendMoneyResponse = objectMapper.readValue(response.getContentAsString(), SendMoneyResponse.class);
        LOG.info("send '" + amount + "' BURST to recipientId: '" + recipientId + "' in '" + sendMoneyResponse.getRequestProcessingTime() + "' ms");
      }
      else
      {
        LOG.error("Error: " + response.getContentAsString());
      }
    }
    catch(Exception e)
    {
      LOG.warn("Error: Failed to 'sendMoney' to accountId '" + recipientId + "' : " + e.getMessage());
    }
    return sendMoneyResponse;
  }

  // if asset has digits amount will start at lowest digit e.g. 2 digits, amount of 15 will be 0.15 assets
  public SendMoneyResponse sendAsset(int amount, String numericAssetId, String recipientId, String secretPhrase)
  {
    SendMoneyResponse sendMoneyResponse = null;
    try
    {
      ContentResponse response = httpClient.POST(BurstcoinFaucetProperties.getWalletServer() + "/burst")
        .param("requestType", "transferAsset")
        .param("asset", numericAssetId)
        .param("recipient", recipientId)
        .param("amountNQT", String.valueOf(amount))
        .param("feeNQT", BurstcoinFaucetProperties.getFee() + "00000000")
        .param("deadline", "1000")
        .param("secretPhrase", secretPhrase)
        .timeout(BurstcoinFaucetProperties.getConnectionTimeout(), TimeUnit.MILLISECONDS)
        .send();

      if(!response.getContentAsString().contains("errorDescription"))
      {
        sendMoneyResponse = objectMapper.readValue(response.getContentAsString(), SendMoneyResponse.class);
        LOG.info("send '" + amount + "' BURST to recipientId: '" + recipientId + "' in '" + sendMoneyResponse.getRequestProcessingTime() + "' ms");
      }
      else
      {
        LOG.error("Error: " + response.getContentAsString());
      }
    }
    catch(Exception e)
    {
      LOG.warn("Error: Failed to 'sendMoney' to accountId '" + recipientId + "' : " + e.getMessage());
    }
    return sendMoneyResponse;
  }

  public int getNumberOfClaims(String recipientId)
  {

    Map<String, Transaction> transactions = new HashMap<>();
    // get all transactions
    boolean hasMoreTransactions = true;
    int offset = 0;
    int transactionsPerRequest = 1999;
    while(hasMoreTransactions)
    {
      Map<String, Transaction> temp = getTransactions(recipientId, offset, transactionsPerRequest, true);
      if(temp != null && !temp.isEmpty())
      {
        hasMoreTransactions = temp.size() >= transactionsPerRequest;
        transactions.putAll(temp);
        offset += transactionsPerRequest;
      }
      else
      {
        hasMoreTransactions = false;
      }
    }

    int count = 0;
    for(Transaction transaction : transactions.values())
    {
      if(FAUCET_ACCOUNT_RS.equals(transaction.getSenderRS()))
      {
        count++;
      }
    }
    return count;
  }

  public Map<String, Transaction> getTransactions(String accountId, int offset, int transactionsPerRequest, boolean ignoreException)
  {
    Map<String, Transaction> transactionLookup = null;
    try
    {
      InputStreamResponseListener listener = new InputStreamResponseListener();

      Request request = httpClient.POST(BURST_API_URL)
        .param("requestType", "getAccountTransactions")
        .param("firstIndex", String.valueOf(offset))
        .param("lastIndex", String.valueOf(offset + transactionsPerRequest))
        .param("account", accountId);
      request.send(listener);

      Response response = listener.get(BurstcoinFaucetProperties.getConnectionTimeout(), TimeUnit.MILLISECONDS);
      transactionLookup = handleGetTransactionsResponse(listener, response, ignoreException);
    }
    catch(Exception e)
    {
      LOG.warn("Error: Failed to 'getAccountTransactions' for accountId '" + accountId + "' : " + e.getMessage());
    }
    return transactionLookup;
  }

  public Map<String, Transaction> getTransactions(String accountId, int timestamp, boolean ignoreException)
  {
    Map<String, Transaction> transactionLookup = null;
    try
    {
      InputStreamResponseListener listener = new InputStreamResponseListener();

      Request request = httpClient.POST(BURST_API_URL)
        .param("requestType", "getAccountTransactions")
        .param("timestamp", String.valueOf(timestamp))
        .param("account", accountId);
      request.send(listener);

      Response response = listener.get(BurstcoinFaucetProperties.getConnectionTimeout(), TimeUnit.MILLISECONDS);
      transactionLookup = handleGetTransactionsResponse(listener, response, ignoreException);
    }
    catch(Exception e)
    {
      LOG.warn("Error: Failed to 'getAccountTransactions' for accountId '" + accountId + "' : " + e.getMessage());
    }
    return transactionLookup;
  }

  private Map<String, Transaction> handleGetTransactionsResponse(InputStreamResponseListener listener, Response response, boolean ignoreException)
  {
    Map<String, Transaction> transactionLookup = new HashMap<>();
    // Look at the response
    if(response.getStatus() == 200)
    {
      // Use try-with-resources to close input stream.
      try (InputStream responseContent = listener.getInputStream())
      {
        Transactions transactions = objectMapper.readValue(responseContent, Transactions.class);
        LOG.info("received '" + transactions.getTransactions().size() + "' transactions in '" + transactions.getRequestProcessingTime() + "' ms");
        transactionLookup = new HashMap<>();
        for(Transaction transaction : transactions.getTransactions())
        {
          transactionLookup.put(transaction.getTransaction(), transaction);
        }
      }
      catch(Exception e)
      {
        // exception is ok for new accounts
        if(!ignoreException)
        {
          LOG.warn("Failed to receive faucet account transactions.", e);
        }
      }
    }
    return transactionLookup;
  }

  public Balance getBalance(String accountId)
  {
    Balance balance = null;
    try
    {
      ContentResponse response = httpClient.POST(BurstcoinFaucetProperties.getWalletServer() + "/burst?requestType=getBalance&account=" + accountId)
        .timeout(BurstcoinFaucetProperties.getConnectionTimeout(), TimeUnit.MILLISECONDS)
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
      LOG.warn("Error: Failed to 'getBalance' for accountId '" + accountId + "' : " + e.getMessage(), e);
    }
    return balance;
  }

  public Timestamp getTime()
  {
    Timestamp timestamp = null;
    try
    {
      ContentResponse response = httpClient.POST(BurstcoinFaucetProperties.getWalletServer() + "/burst?requestType=getTime")
        .timeout(BurstcoinFaucetProperties.getConnectionTimeout(), TimeUnit.MILLISECONDS)
        .send();

      if(!response.getContentAsString().contains("errorDescription"))
      {
        timestamp = objectMapper.readValue(response.getContentAsString(), Timestamp.class);
        LOG.info("received timestamp: '" + timestamp.getTime() + "' in '" + timestamp.getRequestProcessingTime() + "' ms");
      }
      else
      {
        LOG.error("Error: " + response.getContentAsString());
      }
    }
    catch(Exception e)
    {
      LOG.warn("Error: Failed to 'getTime':" + e.getMessage(), e);
    }
    return timestamp;
  }

  public MiningInfo getMiningInfo()
  {
    MiningInfo result = null;
    try
    {
      ContentResponse response;
      response = httpClient.newRequest(BurstcoinFaucetProperties.getWalletServer() + "/burst?requestType=getMiningInfo")
        .timeout(BurstcoinFaucetProperties.getConnectionTimeout(), TimeUnit.MILLISECONDS)
        .send();

      result = objectMapper.readValue(response.getContentAsString(), MiningInfo.class);
    }
    catch(TimeoutException timeoutException)
    {
      LOG.warn("Unable to get mining info caused by connectionTimeout, currently '" + (BurstcoinFaucetProperties.getConnectionTimeout() / 1000)
               + " sec.' try increasing it!");
    }
    catch(Exception e)
    {
      LOG.trace("Unable to get mining info from wallet: " + e.getMessage());
    }
    return result;
  }
}
