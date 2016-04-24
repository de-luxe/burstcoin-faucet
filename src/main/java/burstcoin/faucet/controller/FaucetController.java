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
import burstcoin.faucet.network.NetworkComponent;
import burstcoin.faucet.network.model.Balance;
import burstcoin.faucet.network.model.SendMoneyResponse;
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

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@Controller
public class FaucetController
{
  private static final long claimInterval = 1000 * 60 * 60 * BurstcoinFaucetProperties.getClaimInterval();

  @Autowired
  private RecaptchaValidator recaptchaValidator;

  @Autowired
  private AccountRepository accountRepository;

  @Autowired
  private NetworkComponent networkComponent;

  @RequestMapping("/")
  public String index(Model model)
  {
    byte[] publicKey = Crypto.getPublicKey(BurstcoinFaucetProperties.getPassPhrase());
    byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
    long accountId = Convert.fullHashToId(publicKeyHash);
    String numericFaucetAccountId = Convert.toUnsignedLong(accountId);
    Balance balance = networkComponent.getBalance(numericFaucetAccountId);

    model.addAttribute("faucetAccount", "BURST-" + ReedSolomon.encode(accountId));
    model.addAttribute("faucetBalance", (Long.valueOf(balance.getUnconfirmedBalanceNQT()) / 100000000) + " BURST");
    model.addAttribute("reCaptchaPublicKey", BurstcoinFaucetProperties.getPublicKey());

    return "index";
  }

  @RequestMapping(value = "/", method = RequestMethod.POST)
  @Transactional
  public String claim(@RequestParam("accountId") String accountId, HttpServletRequest request, Model model)
  {
    // convert numeric account id
    if(!accountId.contains("BURST"))
    {
      accountId = "BURST-" + ReedSolomon.encode(Convert.parseUnsignedLong(accountId));
    }

    Account account = getAccount(accountId);

    if(account.getLastClaim() == null || new Date().getTime() > (account.getLastClaim().getTime() + claimInterval))
    {
      if(recaptchaValidator.validate(request).isSuccess())
      {
        SendMoneyResponse sendMoneyResponse = networkComponent.sendMoney(BurstcoinFaucetProperties.getClaimAmount(), accountId,
                                                                         BurstcoinFaucetProperties.getPassPhrase());
        if(sendMoneyResponse != null)
        {
          account.setLastClaim(new Date());
          accountRepository.save(account);

          return "redirect:/?success=" + BurstcoinFaucetProperties.getClaimAmount()+ " BURST send to "+ account.getAccountId();
        }
        else
        {
          return "redirect:/?error=Sry, faucet could not access Wallet, to send your BURST.";
        }
      }
    }
    else
    {
      long time = (account.getLastClaim().getTime() + claimInterval) - new Date().getTime();
      return "redirect:/?error=No BURST send. Please wait at least " + (time / (60*1000)) + " minutes, before claim again.";
    }
    return "redirect:/?error=Unexpected problem ... could not send BURST.";
  }

  private Account getAccount(@RequestParam("accountId") String accountId)
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
}
