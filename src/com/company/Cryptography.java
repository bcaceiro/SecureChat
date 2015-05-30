package com.company;


import javax.crypto.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by jpedrom on 19/05/15.
 */
public class Cryptography {

    private KeyGenerator keygenerator;
    private SecretKey myDesKey;
    private Cipher desCipher;

    public Cryptography () throws NoSuchAlgorithmException, NoSuchPaddingException {

        keygenerator = KeyGenerator.getInstance("DES");
        myDesKey = keygenerator.generateKey();

        // Create the cipher
        desCipher = Cipher.getInstance("DES/ECB/NoPadding");
    }

    private byte[] stringToByte (String s) {

        byte[] text = s.getBytes();

        System.out.println(text.length);

        return text;
    }

    private String byteToString (byte[] text) {

        String message = new String(text);

        return message;
    }

    public String encrypt (String s) {

        /*System.out.println("String: " + s);*/

        byte[] message = stringToByte(s);
        String messageEncrypted = null;

        /*System.out.println("Byte text: " + message);
        System.out.println("Byte to String: " + new String(message));*/

        try {

            desCipher.init(Cipher.ENCRYPT_MODE, myDesKey);
            byte[] textEncrypted = desCipher.doFinal(message);

            messageEncrypted = byteToString(textEncrypted);

        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        /*System.out.println("Byte to String encrypted: " + new String(messageEncrypted));*/

        return messageEncrypted;
    }

    public String decrypt (String s) {

        byte[] message = stringToByte(s);
        String messageDencrypted = null;

        try {

            desCipher.init(Cipher.DECRYPT_MODE, myDesKey);
            byte[] textDecrypted = desCipher.doFinal(message);

        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }


        return messageDencrypted;
    }
}
