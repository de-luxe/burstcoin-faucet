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

package burstcoin.faucet.network.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Transaction
{
  private String senderPublicKey;
  private String signature;
  private String feeNQT;
  private int type;
  private long confirmations;
  private String fullHash;
  private int version;
  private String ecBlockId;
  private String signatureHash;

  @JsonIgnore
  private Object attachment;

  private String senderRS;
  private int subtype;
  private String amountNQT;
  private String sender;
  private String recipientRS;
  private String recipient;
  private int ecBlockHeight;
  private String block;
  private int blockTimestamp;
  private int deadline;
  private String transaction;
  private int timestamp;
  private int height;

  public Transaction()
  {
  }

  public String getSenderPublicKey()
  {
    return senderPublicKey;
  }

  public String getSignature()
  {
    return signature;
  }

  public String getFeeNQT()
  {
    return feeNQT;
  }

  public int getType()
  {
    return type;
  }

  public long getConfirmations()
  {
    return confirmations;
  }

  public String getFullHash()
  {
    return fullHash;
  }

  public int getVersion()
  {
    return version;
  }

  public String getEcBlockId()
  {
    return ecBlockId;
  }

  public String getSignatureHash()
  {
    return signatureHash;
  }

  public Object getAttachment()
  {
    return attachment;
  }

  public String getSenderRS()
  {
    return senderRS;
  }

  public int getSubtype()
  {
    return subtype;
  }

  public String getAmountNQT()
  {
    return amountNQT;
  }

  public String getSender()
  {
    return sender;
  }

  public String getRecipientRS()
  {
    return recipientRS;
  }

  public String getRecipient()
  {
    return recipient;
  }

  public int getEcBlockHeight()
  {
    return ecBlockHeight;
  }

  public String getBlock()
  {
    return block;
  }

  public int getBlockTimestamp()
  {
    return blockTimestamp;
  }

  public int getDeadline()
  {
    return deadline;
  }

  public String getTransaction()
  {
    return transaction;
  }

  public int getTimestamp()
  {
    return timestamp;
  }

  public int getHeight()
  {
    return height;
  }
}
