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

package burstcoin.faucet.service;

import burstcoin.faucet.BurstcoinFaucetProperties;
import burstcoin.faucet.controller.FaucetController;
import burstcoin.faucet.controller.bean.ClaimStat;
import burstcoin.faucet.controller.bean.StatsData;
import burstcoin.faucet.network.NetworkComponent;
import burstcoin.faucet.network.model.Timestamp;
import burstcoin.faucet.network.model.Transaction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@Component
class StatsService
{
  private static Log LOG = LogFactory.getLog(StatsService.class);

  private final NetworkComponent networkComponent;

  private BeanFactory beanFactory;

  private Timer timer;
  private Timestamp lastUpdateTimestamp;
  private Map<String, Transaction> transactions;

  @Autowired
  public StatsService(NetworkComponent networkComponent, BeanFactory beanFactory)
  {
    this.networkComponent = networkComponent;
    this.beanFactory = beanFactory;

    transactions = new HashMap<>();
    timer = new Timer();
  }

  @PostConstruct
  public void init()
  {
    timer.schedule(new TimerTask()
    {
      @Override
      public void run()
      {
        try
        {
          int numberOfTransactionsBeforeUpdate = transactions.size();

          // timestamp of last received transactions
          Integer updateFromTimestamp = lastUpdateTimestamp == null ? null : lastUpdateTimestamp.getTime();

          // remember current timestamp for next update
          lastUpdateTimestamp = networkComponent.getTime();

          if(updateFromTimestamp == null)
          {
            // get all transactions
            boolean hasMoreTransactions = true;
            int offset = 0;
            int transactionsPerRequest = 1999;
            while(hasMoreTransactions)
            {
              Map<String, Transaction> temp = networkComponent
                .getTransactions(BurstcoinFaucetProperties.getNumericFaucetAccountId(), offset, transactionsPerRequest, false);
              if(temp != null && !temp.isEmpty())
              {
                hasMoreTransactions = temp.size() >= transactionsPerRequest;
                transactions.putAll(temp);
                offset += transactionsPerRequest;
              }
              else
              {
                LOG.warn("Failed to get Transactions ... maybe new account or network/wallet issue.");
                hasMoreTransactions = false;
              }
            }
          }
          else
          {
            // get transaction from specific timestamp
            Map<String, Transaction> temp = networkComponent.getTransactions(BurstcoinFaucetProperties.getNumericFaucetAccountId(), updateFromTimestamp, false);
            transactions.putAll(temp);
          }

          // recalculate stats if number of transactions has changed, or there are no transactions
          if(numberOfTransactionsBeforeUpdate < transactions.size() || transactions.size() == 0)
          {
            LOG.info("Updating stats with " + (transactions.size() - numberOfTransactionsBeforeUpdate) + " new transactions ...");
            beanFactory.getBean(FaucetController.class).handleUpdatedStats(generateStatsData(BurstcoinFaucetProperties.getNumericFaucetAccountId(), transactions));
          }
          else
          {
            LOG.info("No stats update needed, no new transactions ...");
          }
        }
        catch(Exception e)
        {
          LOG.error("Failed update stats data: ", e);
        }
      }
    }, 10000, BurstcoinFaucetProperties.getStatsUpdateInterval());
  }

  private static StatsData generateStatsData(String accountId, Map<String, Transaction> transactions)
  {
    StatsData data = new StatsData();

    for(Transaction transaction : transactions.values())
    {
      if(accountId.equals(transaction.getSender()))
      {
        if(transaction.getRecipientRS() != null)
        {
          data.addToTotalClaims(Long.valueOf(transaction.getAmountNQT()));

          ClaimStat claimStat = data.getClaimLookup().get(transaction.getRecipientRS());
          if(claimStat == null)
          {
            claimStat = new ClaimStat(transaction.getTimestamp(), Long.valueOf(transaction.getAmountNQT()), 1, transaction.getRecipientRS());
          }
          else
          {
            // update claim amount
            Long accountClaims = claimStat.getClaimed();
            accountClaims = accountClaims + Long.valueOf(transaction.getAmountNQT());
            claimStat.setClaimed(accountClaims);
            // update timestamp
            if(claimStat.getLastClaimTimestamp() < transaction.getTimestamp())
            {
              claimStat.setLastClaimTimestamp(transaction.getTimestamp());
            }
            // update number
            claimStat.setNumberOfClaims(1 + claimStat.getNumberOfClaims());
          }
          data.getClaimLookup().put(transaction.getRecipientRS(), claimStat);
        }
      }
      else
      {
        data.addToTotalDonations(Long.valueOf(transaction.getAmountNQT()));
        Long accountDonations = data.getDonateLookup().get(transaction.getSenderRS());
        accountDonations = accountDonations == null ? Long.valueOf(transaction.getAmountNQT()) : accountDonations + Long.valueOf(transaction.getAmountNQT());
        data.getDonateLookup().put(transaction.getSenderRS(), accountDonations);
      }
    }

    // cleanup
    BigInteger minDonationAmount = new BigInteger(BurstcoinFaucetProperties.getMinDonationAmount() + "00000000");
    for(Map.Entry<String, Long> donateEntry : data.getDonateLookup().entrySet())
    {
      if(minDonationAmount.compareTo(new BigInteger(donateEntry.getValue() +"")) > 0)
      {
        data.addToOtherDonations(donateEntry.getValue());
        data.getOthers().add(donateEntry.getKey());
      }
    }

    for(String other : data.getOthers())
    {
      data.getDonateLookup().remove(other);
    }
    return data;
  }
}
