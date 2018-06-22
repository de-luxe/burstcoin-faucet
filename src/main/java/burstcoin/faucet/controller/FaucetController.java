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
import burstcoin.faucet.controller.bean.AccountCheck;
import burstcoin.faucet.controller.bean.ClaimStat;
import burstcoin.faucet.controller.bean.StatsData;
import burstcoin.faucet.data.Account;
import burstcoin.faucet.data.AccountRepository;
import burstcoin.faucet.data.IPAddress;
import burstcoin.faucet.data.IPAddressRepository;
import burstcoin.faucet.network.NetworkComponent;
import burstcoin.faucet.network.model.Balance;
import burstcoin.faucet.network.model.MiningInfo;
import burstcoin.faucet.network.model.SendMoneyResponse;
import com.github.mkopylec.recaptcha.validation.RecaptchaValidationException;
import com.github.mkopylec.recaptcha.validation.RecaptchaValidator;
import com.github.mkopylec.recaptcha.validation.ValidationResult;
import nxt.crypto.ReedSolomon;
import nxt.util.Convert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Controller
public class FaucetController
{
  private static final long claimInterval = 1000 * 60 * 60 * BurstcoinFaucetProperties.getClaimInterval();
  private static final String SECURE_COOKIE_NAME = "none";
  private static Log LOG = LogFactory.getLog(FaucetController.class);

  private final RecaptchaValidator recaptchaValidator;
  private final AccountRepository accountRepository;
  private final IPAddressRepository ipAddressRepository;
  private final NetworkComponent networkComponent;
  private final MessageSource messageSource;

  private String faucetAccountRS;
  private Set<Character> alphabet;
  private StatsData stats;

  @Autowired
  public FaucetController(AccountRepository accountRepository, NetworkComponent networkComponent, RecaptchaValidator recaptchaValidator,
                          MessageSource messageSource, IPAddressRepository ipAddressRepository)
  {
    this.accountRepository = accountRepository;
    this.networkComponent = networkComponent;
    this.recaptchaValidator = recaptchaValidator;
    this.messageSource = messageSource;
    this.ipAddressRepository = ipAddressRepository;

    alphabet = new HashSet<>(
      Arrays.asList('2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S',
                    'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '-'));

    faucetAccountRS = "BURST-" + ReedSolomon.encode(Convert.parseUnsignedLong(BurstcoinFaucetProperties.getNumericFaucetAccountId()));
  }

  public void handleUpdatedStats(StatsData statsData)
  {
    this.stats = statsData;
  }

  @RequestMapping(value = "/burst", produces = "application/json")
  @ResponseBody
  public MiningInfo burst(@RequestParam("requestType") String requestType)
  {
    if("getMiningInfo".equals(requestType))
    {
      return networkComponent.getMiningInfo();
    }
    return null;
  }

  @RequestMapping("/")
  public String index(HttpServletRequest request, Model model)
  {
    if(stats != null)
    {
      Locale locale = request.getLocale();
      Balance balance = networkComponent.getBalance(BurstcoinFaucetProperties.getNumericFaucetAccountId());

      model.addAttribute("reCaptchaPublicKey", BurstcoinFaucetProperties.getPublicKey());
      model.addAttribute("analyticsCode", BurstcoinFaucetProperties.getAnalyticsCode());

      List<String> donations = new ArrayList<>();
      for(Map.Entry<String, Long> donateEntry : stats.getDonateLookup().entrySet())
      {
        donations.add(messageSource.getMessage("faucet.stats.donations.account",
                                               new Object[]{donateEntry.getKey(), cleanAmount(donateEntry.getValue())}, locale));
      }
      donations.add(messageSource.getMessage("faucet.stats.donations.other", new Object[]{cleanAmount(stats.getOthersDonations())}, locale));

      // no lookup needed anymore, add to list an sort
      List<ClaimStat> claimStats = new ArrayList<>(stats.getClaimLookup().values());
      Collections.sort(claimStats);
      List<String> claims = new ArrayList<>(claimStats.size());

      Iterator<ClaimStat> iterator = claimStats.iterator();
      while(iterator.hasNext() && claims.size() < BurstcoinFaucetProperties.getStatsAmountOfClaimsToShow())
      {
        ClaimStat claimStat = iterator.next();
        claims.add(messageSource.getMessage("faucet.stats.claims.account",
                                            new Object[]{claimStat.getNumberOfClaims(), claimStat.getAccountRS(), cleanAmount(claimStat.getClaimed())},
                                            locale));
      }

      model.addAttribute("totalClaimed", messageSource.getMessage("faucet.stats.claims.summary",
                                                                  new Object[]{cleanAmount(stats.getTotalClaims()), claimStats.size()}, locale));
      model.addAttribute("totalDonated", messageSource.getMessage("faucet.stats.donations.summary",
                                                                  new Object[]{cleanAmount(stats.getTotalDonations()),
                                                                               stats.getDonateLookup().size() + stats.getOthers().size()}, locale));
      model.addAttribute("donations", donations);
      model.addAttribute("claims", claims);

      model.addAttribute("linkUrl", messageSource.getMessage("faucet.link.url", null, locale));
      long cleanAmount = cleanAmount(Long.valueOf(balance != null ? balance.getUnconfirmedBalanceNQT() : "0"));
      model.addAttribute("linkText", messageSource.getMessage("faucet.link.text", new Object[]{faucetAccountRS, cleanAmount}, locale));
      return "index";
    }
    else
    {
      return "init";
    }
  }

  @RequestMapping(value = "/", method = RequestMethod.POST)
  @Transactional
  public String claim(@RequestParam("accountId") String accountId, HttpServletRequest request, HttpServletResponse response)
  {
    Locale locale = request.getLocale();

    // warn on empty faucet
    Balance balance = networkComponent.getBalance(BurstcoinFaucetProperties.getNumericFaucetAccountId());
    long cleanAmount = Long.valueOf(balance != null ? balance.getUnconfirmedBalanceNQT() : "0");
    if(cleanAmount <= BurstcoinFaucetProperties.getFee() + BurstcoinFaucetProperties.getClaimAmount())
    {
      return "redirect:/?error=" + urlEncode(messageSource.getMessage("faucet.error.noFunds", new Object[]{}, locale));
    }

    // check recaptcha
    String ipString = request.getHeader("X-FORWARDED-FOR");
    if(ipString == null)
    {
      ipString = request.getRemoteAddr();
    }

    ValidationResult validationResult = null;
    int retry = 0;

    while(validationResult == null && retry < 2)
    {
      try
      {
        if(ipString != null)
        {
          validationResult = recaptchaValidator.validate(request, ipString);
        }
        else
        {
          validationResult = recaptchaValidator.validate(request);
        }
      }
      catch(RecaptchaValidationException rve)
      {
        LOG.warn("Could not validate recaptcha due recaptchaValidator exception. Retry in 3 sec.");

        try
        {
          Thread.sleep(3000);
          retry++;
        }
        catch(InterruptedException e)
        {
        }
      }
    }


    if(validationResult != null)
    {
      if(validationResult.isSuccess())
      {
        // check input
        AccountCheck accountCheck = checkAccountId(accountId, locale);
        if(!accountCheck.isSuccess())
        {
          return "redirect:/?error=" + urlEncode(accountCheck.getError());
        }
        else
        {
          // check number of claims
          if(BurstcoinFaucetProperties.getMaxClaimsPerAccount() == null
             || BurstcoinFaucetProperties.getMaxClaimsPerAccount() > networkComponent.getNumberOfClaims(accountCheck.getAccountId()))
          {
            // check account last claim
            Account account = getAccount(accountCheck.getAccountId());
            if(account.getLastClaim() == null || new Date().getTime() > (account.getLastClaim().getTime() + claimInterval))
            {
              // check ip last claim
              IPAddress ipAddress = null;
              if(ipString != null)
              {
                ipAddress = getIPAddress(ipString);
              }

              if(ipAddress == null || ipAddress.getLastClaim() == null || new Date().getTime() > (ipAddress.getLastClaim().getTime() + claimInterval))
              {
                // get cookie lastClaim
                Long lastClaimCookie = null;
                Cookie[] cookieArray = request.getCookies();
                if(cookieArray != null)
                {
                  List<Cookie> cookies = Arrays.asList(cookieArray);
                  for(Cookie cookie : cookies)
                  {
                    if(SECURE_COOKIE_NAME.equals(cookie.getName()))
                    {
                      try
                      {
                        lastClaimCookie = Long.valueOf(cookie.getValue());
                      }
                      catch(Exception e)
                      {
                        lastClaimCookie = null;
                      }
                    }
                  }
                }

                // check cookie last claim
                if(lastClaimCookie == null || new Date().getTime() > (lastClaimCookie + claimInterval))
                {
                  SendMoneyResponse sendMoneyResponse = networkComponent.sendMoney(BurstcoinFaucetProperties.getClaimAmount(), account.getAccountId(),
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

                    Cookie lastClaim = new Cookie(SECURE_COOKIE_NAME, String.valueOf(new Date().getTime()));
                    lastClaim.setMaxAge(60 * 60 * 24 * 90);
                    response.addCookie(lastClaim);

                    String[] parameters = {String.valueOf(BurstcoinFaucetProperties.getClaimAmount()), account.getAccountId()};
                    return "redirect:/?success=" + urlEncode(messageSource.getMessage("faucet.msg.send.money", parameters, locale));
                  }
                  else
                  {
                    LOG.info("Claim failed by 'WALLET_ACCESS', account '" + account.getAccountId() + "'.");

                    return "redirect:/?error=" + urlEncode(messageSource.getMessage("faucet.error.claim.walletAccess", null, locale));
                  }
                }
                else
                {
                  LOG.info("Claim denied by COOKIE, account '" + account.getAccountId() + "'.");

                  long cookieTime = (lastClaimCookie + claimInterval) - new Date().getTime();
                  return "redirect:/?error=" + urlEncode(messageSource.getMessage("faucet.error.claim.denied.cookie",
                                                                                  new Object[]{(cookieTime) / (60 * 1000)}, locale));
                }
              }
              else
              {
                LOG.info("Claim denied by IP, account '" + account.getAccountId() + "'.");

                long ipTime = (ipAddress.getLastClaim().getTime() + claimInterval) - new Date().getTime();
                return "redirect:/?error=" + urlEncode(messageSource.getMessage("faucet.error.claim.denied.ip", new Object[]{(ipTime) / (60 * 1000)}, locale));
              }
            }
            else
            {
              LOG.info("Claim denied by ACCOUNT, account '" + account.getAccountId() + "'.");

              long accountTime = (account.getLastClaim().getTime() + claimInterval) - new Date().getTime();
              return "redirect:/?error=" + urlEncode(messageSource.getMessage("faucet.error.claim.denied.account",
                                                                              new Object[]{(accountTime) / (60 * 1000)}, locale));
            }
          }
          else
          {
            LOG.info("Claim denied by NUMBER_OF_CLAIMS, account '" + accountCheck.getAccountId() + "'.");

            return "redirect:/?error=" + urlEncode(messageSource.getMessage("faucet.error.claim.denied.numberOfClaims",
                                                                            new Object[]{(BurstcoinFaucetProperties.getMaxClaimsPerAccount())}, locale));
          }
        }
      }
      else
      {
        LOG.info("Claim failed by 'RECAPTCHA', account '" + accountId + "'.");
        return "redirect:/?error=" + urlEncode(messageSource.getMessage("faucet.error.claim.reCaptcha", null, locale));
      }
    }
    else
    {
      LOG.info("Claim failed by 'RECAPTCHA_VALIDATOR_ERROR', account '" + accountId + "'.");
      return "redirect:/?error=" + urlEncode("There was a error on google server, validating recaptcha ... please try again later or with another browser.");
    }
  }

  private String urlEncode(String text)
  {
    try
    {
      return URLEncoder.encode(text, "UTF-8");
    }
    catch(UnsupportedEncodingException e)
    {
      LOG.error("Could not URL encode: " + text);
      return text;
    }
  }

  private AccountCheck checkAccountId(String accountId, Locale locale)
  {
    AccountCheck accountCheck = null;
    if(StringUtils.isEmpty(accountId) || accountId.length() < 15)
    {
      LOG.info("Claim failed by 'ADDRESS_INVALID', malformed account: '" + obfuscateInput(accountId) + "'.");
      accountCheck = new AccountCheck(accountId, messageSource.getMessage("faucet.error.accountId.malformed", new Object[]{accountId}, locale));
    }
    else
    {
      // get rid of leading/ending space
      accountId = accountId.trim();

      // convert numeric account id
      if(!accountId.contains("BURST"))
      {
        try
        {
          Long unsignedLong = Convert.parseUnsignedLong(accountId);
          accountId = "BURST-" + ReedSolomon.encode(unsignedLong);
          accountCheck = new AccountCheck(accountId);
        }
        catch(Exception e)
        {
          LOG.info("Claim failed by 'ADDRESS_RS_CONVERT', account: '" + obfuscateInput(accountId) + "'.");
          accountCheck = new AccountCheck(accountId, messageSource.getMessage("faucet.error.accountId.rsConvert", new Object[]{accountId}, locale));
        }
      }
      else
      {
        // verify alphabet
        for(char c : accountId.toCharArray())
        {
          if(!alphabet.contains(c))
          {
            LOG.info("Claim failed by 'ADDRESS_ALPHABET', malformed account: '" + obfuscateInput(accountId) + "'.");
            accountCheck = new AccountCheck(accountId, messageSource.getMessage("faucet.error.accountId.alphabet", new Object[]{accountId}, locale));
          }
        }

        if(accountCheck == null)
        {
          accountCheck = new AccountCheck(accountId);
        }
      }
    }
    return accountCheck;
  }

  // if user enters passPhrase due mistake, it will not complete show up in logs
  private String obfuscateInput(String accountIdInput)
  {
    if(accountIdInput == null || accountIdInput.length() < 4)
    {
      return accountIdInput;
    }
    return accountIdInput.substring(0, accountIdInput.length() / 2) + "...obfuscated...";
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

  private long cleanAmount(Long amount)
  {
    return amount / 100000000;
  }
}
