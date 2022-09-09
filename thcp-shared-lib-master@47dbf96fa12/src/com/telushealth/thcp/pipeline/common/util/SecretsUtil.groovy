package com.telushealth.thcp.pipeline.common.util

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import org.apache.commons.io.FileUtils
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.KeyTransRecipientInformation;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

public class SecretsUtil implements Serializable
{

    public static void main(String[] args) throws Exception
    {

        String privateKeyFileName = "c:\\private_key.pkcs7.pem";
        String content = FileUtils.readFileToString(new File (privateKeyFileName), "UTF-8");
        
//        println content;
        
        byte[] bytes = decryptString(content, "ENC[PKCS7,MIIBmQYJKoZIhvcNAQcDoIIBijCCAYYCAQAxggEhMIIBHQIBADAFMAACAQEwDQYJKoZIhvcNAQEBBQAEggEAUJQlAkFa7x4v4ojJQnBM4xybR3HnUNqjoEpeNwOdJ3pQZQTPOM6wQlSxD4KrVEOOfcryo9JfW02i5vms2mivj7Yh/qC6TPD/2d1DloiVii9ypThcA4pHMkZZpSdNmZSfTXR8MWiumNOpWu+x6aBPVwHUXlQRFph0h9iBPRgnTdo5WhIRfE/tpy2D1GD9GqV13dTmD2etTjyGtvvN53ik7ytVjvUHX2mPYwUBguNyMzsPkCABgIHIsR0TPyaVNlC6wXO8zk3hzbP5as28lU+IS5YPIs3/NWpSfpDZUdL4e8KDxt5n2cUSJ9mBX7TmTT5/PHOi6WFHCx1Hxv2ilyXcADBcBgkqhkiG9w0BBwEwHQYJYIZIAWUDBAEqBBD5UdbgTzbZrjYxHCXihhSWgDDNsCkwaroQZjZDoMGv44W+Br23Y2vMxk5Baa3DHN5KxhIFL5npfXjHhu67qPG3Zf0=]");
        println new String(bytes);

    }

    public static byte[] decryptString(String privateKeyContents, String encryptedString)
            throws InvalidKeySpecException, IOException, NoSuchAlgorithmException, CMSException
    {
        java.security.Security.addProvider(
                new org.bouncycastle.jce.provider.BouncyCastleProvider());
        PrivateKey privateKey = readPKCSPrivateKey(privateKeyContents);
        encryptedString = encryptedString.replace("ENC[PKCS7,", "");
        if(encryptedString.endsWith("]")) {
            encryptedString = encryptedString.substring(0,encryptedString.length() - 1);
        }
        byte[] input = Base64.getDecoder().decode(encryptedString.getBytes());
        return decryptData(input, privateKey);
    }
    
    public static byte[] encryptString(String publicKeyFileName, String plainString)
            throws IOException, CMSException, CertificateException, NoSuchProviderException
    {
        java.security.Security.addProvider(
                new org.bouncycastle.jce.provider.BouncyCastleProvider());
        
        byte[] input = Base64.getDecoder().decode(plainString.getBytes());
        X509Certificate certificate = readX509PublicKey(publicKeyFileName);
        return encryptData(input, certificate);
    }

    private static X509Certificate readX509PublicKey(String fileName)
            throws CertificateException, FileNotFoundException, NoSuchProviderException
    {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509", "BC");
        X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(new FileInputStream(fileName));
        return certificate;
    }

    private static PrivateKey readPKCSPrivateKey(String fileContents)
            throws InvalidKeySpecException, IOException, NoSuchAlgorithmException
    {
        KeyFactory factory = KeyFactory.getInstance("RSA");
        Reader inputString = new StringReader(fileContents);
        BufferedReader reader = new BufferedReader(inputString);
        PemReader pemReader = new PemReader(reader);
        PemObject pemObject = pemReader.readPemObject();
        byte[] content = pemObject.getContent();
        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
        return factory.generatePrivate(privKeySpec);
    }

    private static byte[] encryptData(final byte[] data, X509Certificate encryptionCertificate)
            throws CertificateEncodingException, CMSException, IOException
    {
        byte[] encryptedData = null;
        if (null != data && null != encryptionCertificate)
        {
            CMSEnvelopedDataGenerator cmsEnvelopedDataGenerator = new CMSEnvelopedDataGenerator();
            JceKeyTransRecipientInfoGenerator jceKey = new JceKeyTransRecipientInfoGenerator(encryptionCertificate);
            cmsEnvelopedDataGenerator.addRecipientInfoGenerator(jceKey);
            CMSTypedData msg = new CMSProcessableByteArray(data);
            OutputEncryptor encryptor = new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES256_CBC).setProvider("BC")
                    .build();
            CMSEnvelopedData cmsEnvelopedData = cmsEnvelopedDataGenerator.generate(msg, encryptor);
            encryptedData = cmsEnvelopedData.getEncoded();
        }
        return encryptedData;
    }

    private static byte[] decryptData(final byte[] encryptedData, final PrivateKey decryptionKey) throws CMSException
    {
        byte[] decryptedData = null;
        if (null != encryptedData && null != decryptionKey)
        {
            CMSEnvelopedData envelopedData = new CMSEnvelopedData(encryptedData);
            Collection<RecipientInformation> recip = envelopedData.getRecipientInfos().getRecipients();
            KeyTransRecipientInformation recipientInfo = (KeyTransRecipientInformation) recip.iterator().next();
            JceKeyTransRecipient recipient = new JceKeyTransEnvelopedRecipient(decryptionKey);
            decryptedData = recipientInfo.getContent(recipient);
        }
        return decryptedData;
    }
    
    

}
