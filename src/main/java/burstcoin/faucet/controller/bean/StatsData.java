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

package burstcoin.faucet.controller.bean;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StatsData
{
  private long totalClaims;
  private long totalDonations;
  private long othersDonations;
  private Map<String, ClaimStat> claimLookup;
  private Map<String, Long> donateLookup;
  private Set<String> others;

  public StatsData()
  {
    totalClaims = 0;
    totalDonations = 0;
    othersDonations = 0;

    claimLookup = new HashMap<>();
    donateLookup = new HashMap<>();
    others = new HashSet<>();
  }

  public Set<String> getOthers()
  {
    return others;
  }

  public long getOthersDonations()
  {
    return othersDonations;
  }

  public Map<String, ClaimStat> getClaimLookup()
  {
    return claimLookup;
  }

  public Map<String, Long> getDonateLookup()
  {
    return donateLookup;
  }

  public long getTotalClaims()
  {
    return totalClaims;
  }

  public long getTotalDonations()
  {
    return totalDonations;
  }

  public void addToTotalClaims(Long claimAmount)
  {
    totalClaims += claimAmount;
  }

  public void addToTotalDonations(Long donationAmount)
  {
    totalDonations += donationAmount;
  }

  public void addToOtherDonations(Long otherDonationAmount)
  {
    othersDonations += otherDonationAmount;
  }
}
