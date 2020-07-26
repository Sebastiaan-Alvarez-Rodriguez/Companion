package com.python.companion.security;

public class EncryptionTuple {
    public String ciphertext;
    public byte[] iv;

    public EncryptionTuple(String ciphertext, byte[] iv) {
        this.ciphertext = ciphertext;
        this.iv = iv;
    }

    public EncryptionTuple() {}
}
