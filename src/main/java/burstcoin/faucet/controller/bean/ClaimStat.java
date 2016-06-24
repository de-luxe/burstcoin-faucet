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

public class ClaimStat
  implements Comparable<ClaimStat>
{
  private Integer lastClaimTimestamp;
  private Long claimed;
  private int numberOfClaims;

  private String accountRS;

  public ClaimStat(Integer lastClaimTimestamp, Long claimed, int numberOfClaims, String accountRS)
  {
    this.lastClaimTimestamp = lastClaimTimestamp;
    this.claimed = claimed;
    this.numberOfClaims = numberOfClaims;
    this.accountRS = accountRS;
  }

  public Integer getLastClaimTimestamp()
  {
    return lastClaimTimestamp;
  }

  public Long getClaimed()
  {
    return claimed;
  }

  public void setClaimed(Long claimed)
  {
    this.claimed = claimed;
  }

  public int getNumberOfClaims()
  {
    return numberOfClaims;
  }

  public String getAccountRS()
  {
    return accountRS;
  }

  public void setLastClaimTimestamp(Integer lastClaimTimestamp)
  {
    this.lastClaimTimestamp = lastClaimTimestamp;
  }

  public void setNumberOfClaims(int numberOfClaims)
  {
    this.numberOfClaims = numberOfClaims;
  }

  @Override
  public boolean equals(Object o)
  {
    if(this == o)
    {
      return true;
    }
    if(!(o instanceof ClaimStat))
    {
      return false;
    }

    ClaimStat claimStat = (ClaimStat) o;

    return accountRS.equals(claimStat.accountRS);
  }

  @Override
  public int hashCode()
  {
    return accountRS.hashCode();
  }

  @Override
  public int compareTo(ClaimStat o)
  {
    return o.getLastClaimTimestamp().compareTo(lastClaimTimestamp);
  }
}
