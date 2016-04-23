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

package burstcoin.faucet.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.Date;

@Entity
public class Account
  implements Serializable
{
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue
  private Long id;

  @Column(nullable = false, unique = true)
  private String accountId;

  @Column(nullable = true)
  private Date lastClaim;

  protected Account()
  {
  }

  public Account(String accountId, Date lastClaim)
  {
    this.accountId = accountId;
    this.lastClaim = lastClaim;
  }

  public Long getId()
  {
    return id;
  }

  public void setAccountId(String accountId)
  {
    this.accountId = accountId;
  }

  public void setLastClaim(Date lastClaim)
  {
    this.lastClaim = lastClaim;
  }

  public String getAccountId()
  {
    return accountId;
  }

  public Date getLastClaim()
  {
    return lastClaim;
  }
}
