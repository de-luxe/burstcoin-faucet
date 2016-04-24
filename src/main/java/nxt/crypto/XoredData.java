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

package nxt.crypto;

public final class XoredData {

    private final byte[] data;
    private final byte[] nonce;

    public XoredData(byte[] data, byte[] nonce) {
        this.data = data;
        this.nonce = nonce;
    }

    public static XoredData encrypt(byte[] plaintext, byte[] myPrivateKey, byte[] theirPublicKey) {
        byte[] nonce = Crypto.xorEncrypt(plaintext, 0, plaintext.length, myPrivateKey, theirPublicKey);
        return new XoredData(plaintext, nonce);
    }

    public byte[] decrypt(byte[] myPrivateKey, byte[] theirPublicKey) {
        byte[] ciphertext = new byte[getData().length];
        System.arraycopy(getData(), 0, ciphertext, 0, ciphertext.length);
        Crypto.xorDecrypt(ciphertext, 0, ciphertext.length, myPrivateKey, theirPublicKey, getNonce());
        return ciphertext;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getNonce() {
        return nonce;
    }

}
