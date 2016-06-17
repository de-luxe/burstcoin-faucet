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

package burstcoin.faucet.controller;

import burstcoin.faucet.BurstcoinFaucetProperties;
import burstcoin.faucet.data.Account;
import burstcoin.faucet.data.AccountRepository;
import burstcoin.faucet.data.IPAddress;
import burstcoin.faucet.data.IPAddressRepository;
import burstcoin.faucet.network.NetworkComponent;
import burstcoin.faucet.network.model.Balance;
import burstcoin.faucet.network.model.SendMoneyResponse;
import burstcoin.faucet.network.model.Transaction;
import com.github.mkopylec.recaptcha.validation.RecaptchaValidator;
import nxt.crypto.Crypto;
import nxt.crypto.ReedSolomon;
import nxt.util.Convert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class FaucetController
{
  private static final long claimInterval = 1000 * 60 * 60 * BurstcoinFaucetProperties.getClaimInterval();

  @Autowired
  private RecaptchaValidator recaptchaValidator;

  @Autowired
  private AccountRepository accountRepository;

  @Autowired
  private IPAddressRepository ipAddressRepository;

  @Autowired
  private NetworkComponent networkComponent;

  private String numericFaucetAccountId;
  private String faucetAccountRS;
  private Set<Character> alphabet;
  private Stats stats;
  private Date lastStatsUpdate;

  @PostConstruct
  private void init()
  {
    alphabet = new HashSet<>(
      Arrays.asList('2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S',
                    'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '-'));

    byte[] publicKey = Crypto.getPublicKey(BurstcoinFaucetProperties.getPassPhrase());
    byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
    long faucetAccountId = Convert.fullHashToId(publicKeyHash);
    numericFaucetAccountId = Convert.toUnsignedLong(faucetAccountId);
    faucetAccountRS = "BURST-" + ReedSolomon.encode(faucetAccountId);
  }

  @RequestMapping("/")
  public String index(Model model)
  {
    Balance balance = networkComponent.getBalance(numericFaucetAccountId);

    model.addAttribute("faucetAccount", faucetAccountRS);
    model.addAttribute("faucetBalance", (Long.valueOf(balance.getUnconfirmedBalanceNQT()) / 100000000) + " BURST");
    model.addAttribute("reCaptchaPublicKey", BurstcoinFaucetProperties.getPublicKey());

    if(lastStatsUpdate == null || lastStatsUpdate.getTime() < new Date().getTime() - (1000 * 60 * 5 /* 5 minutes */))
    {
      stats = getStats(numericFaucetAccountId);
      lastStatsUpdate = new Date();
    }

    model.addAttribute("totalClaimed", stats.getTotalClaimed());
    model.addAttribute("totalDonated", stats.getTotalDonated());
    model.addAttribute("donations", stats.getDonations());
    model.addAttribute("claims", stats.getClaims());

    return "index";
  }

  @RequestMapping(value = "/", method = RequestMethod.POST)
  @Transactional
  public String claim(@RequestParam("accountId") String accountId, HttpServletRequest request)
  {
    // get rid of leading/ending space
    accountId = accountId.trim();

    // convert numeric account id
    if(!accountId.contains("BURST"))
    {
      Long unsignedLong;
      try
      {
        unsignedLong = Convert.parseUnsignedLong(accountId);
      }
      catch(Exception e)
      {
        return "redirect:/?error=Please enter you BURST 'Account ID' or 'Numeric Account ID'!";
      }
      accountId = "BURST-" + ReedSolomon.encode(unsignedLong);
    }

    // verify alphabet
    for(char c : accountId.toCharArray())
    {
      if(!alphabet.contains(c))
      {
        return "redirect:/?error=Please enter you BURST 'Account ID' or 'Numeric Account ID'!";
      }
    }

    // check account
    Account account = getAccount(accountId);
    if(account.getLastClaim() == null || new Date().getTime() > (account.getLastClaim().getTime() + claimInterval))
    {
      // check ip
      String ip = request.getHeader("X-FORWARDED-FOR");
      if(ip == null)
      {
        ip = request.getRemoteAddr();
      }

      IPAddress ipAddress = null;
      if(ip != null)
      {
        ipAddress = getIPAddress(ip);
      }

      if(ipAddress == null || ipAddress.getLastClaim() == null || new Date().getTime() > (ipAddress.getLastClaim().getTime() + claimInterval))
      {
        // check recaptcha
        if(recaptchaValidator.validate(request).isSuccess())
        {
          SendMoneyResponse sendMoneyResponse = networkComponent.sendMoney(BurstcoinFaucetProperties.getClaimAmount(), accountId,
                                                                           BurstcoinFaucetProperties.getPassPhrase());
          if(sendMoneyResponse != null)
          {
            // update ip
            if(ipAddress != null)
            {
              ipAddress.setLastClaim(new Date());
              ipAddressRepository.save(ipAddress);
            }

            // update account
            account.setLastClaim(new Date());
            accountRepository.save(account);

            return "redirect:/?success=" + BurstcoinFaucetProperties.getClaimAmount() + " BURST send to " + account.getAccountId();
          }
          else
          {
            return "redirect:/?error=Sry, faucet could not access Wallet, to send your BURST.";
          }
        }
      }
      else
      {
        long ipTime = (ipAddress.getLastClaim().getTime() + claimInterval) - new Date().getTime();
        return "redirect:/?error=No BURST send. Please wait at least " + ((ipTime) / (60 * 1000))
               + " minutes, before claim again with your accounts.";
      }
    }
    else
    {
      long accountTime = (account.getLastClaim().getTime() + claimInterval) - new Date().getTime();
      return "redirect:/?error=No BURST send. Please wait at least " + ((accountTime) / (60 * 1000))
             + " minutes, before claim again.";
    }
    return "redirect:/?error=Unexpected problem ... could not send BURST.";
  }

  private Account getAccount(String accountId)
  {
    Account account = accountRepository.findAccountByAccountId(accountId);
    if(account == null)
    {
      // create account
      account = new Account(accountId, null);
      accountRepository.save(account);
    }
    return account;
  }

  private IPAddress getIPAddress(String ip)
  {
    IPAddress ipAddress = ipAddressRepository.findIPAddressByIp(ip);
    if(ipAddress == null)
    {
      // create account
      ipAddress = new IPAddress(ip, null);
      ipAddressRepository.save(ipAddress);
    }
    return ipAddress;
  }

  private Stats getStats(String accountId)
  {
    Map<String, Transaction> transactions = networkComponent.getTransactions(accountId);

    long totalClaims = 0;
    long totalDonations = 0;

    Map<String, ClaimStat> claimLookup = new HashMap<>();
    Map<String, Long> donateLookup = new HashMap<>();

    for(Transaction transaction : transactions.values())
    {
      if(accountId.equals(transaction.getSender()))
      {
        if(transaction.getRecipientRS() != null)
        {
          totalClaims += Long.valueOf(transaction.getAmountNQT());

          ClaimStat claimStat = claimLookup.get(transaction.getRecipientRS());
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
          claimLookup.put(transaction.getRecipientRS(), claimStat);
        }
      }
      else
      {
        totalDonations += Long.valueOf(transaction.getAmountNQT());

        Long accountDonations = donateLookup.get(transaction.getSenderRS());
        accountDonations = accountDonations == null ? Long.valueOf(transaction.getAmountNQT()) : accountDonations + Long.valueOf(transaction.getAmountNQT());
        donateLookup.put(transaction.getSenderRS(), accountDonations);
      }
    }

    // cleanup
    Long othersDonations = 0L;
    Set<String> others = new HashSet<>();
    for(Map.Entry<String, Long> donateEntry : donateLookup.entrySet())
    {
      if(donateEntry.getValue() < 1000000000) // add donate accounts with less than 10 BURST to others
      {
        othersDonations += donateEntry.getValue();
        others.add(donateEntry.getKey());
      }
    }
    for(String other : others)
    {
      donateLookup.remove(other);
    }

    List<String> donations = new ArrayList<>();
    for(Map.Entry<String, Long> donateEntry : donateLookup.entrySet())
    {
      donations.add(donateEntry.getKey() + " - " + (donateEntry.getValue() / 100000000) + " BURST");
    }
    donations.add("Other Accounts - " + (othersDonations / 100000000) + " BURST");

    // no lookup needed anymore, add to list an sort
    List<ClaimStat> claimStats = new ArrayList<>(claimLookup.values());
    Collections.sort(claimStats);
    List<String> claims = new ArrayList<>(claimStats.size());
    for(ClaimStat claimStat : claimStats)
    {
      claims.add(claimStat.getNumberOfClaims() + "x " + claimStat.getAccountRS() + " - " + (claimStat.getClaimed() / 100000000) + " BURST");
    }

    return new Stats("Faucet received " + (totalDonations / 100000000) + " BURST from " + (donateLookup.size() + others.size()) + " Accounts.",
                     "Faucet send " + (totalClaims / 100000000) + " BURST to " + claims.size() + " Accounts.", donations, claims);
  }
}
