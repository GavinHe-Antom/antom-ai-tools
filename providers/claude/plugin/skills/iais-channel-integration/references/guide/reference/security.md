# Platform security API reference

[Security responsibilities](../05-security.md) · [Guide index](../README.md)

Interface: `PlatformChannelSecurityService`. Implemented by the platform and injected into the Adapter; it is not an institution SPI to implement.
The following reflects the SDK source; the delivered JAR must match. Each operation has an independent request class. Callers do not pass a subject, tenant, or key version.

Security requests still explicitly require merchantId. Read it from `ChannelRequestContext.current().getMerchantId()`, not from the business body, Order.merchant, or institution Client-Id. CLI-generated security wrappers populate this value; protocol hooks supply only content, signatures, and algorithm parameters, without selecting a merchant. Direct SDK security calls must also pass the context merchant explicitly; digest does not require one. Missing context or merchant must fail, without a default-merchant fallback. See [Request context](context.md).

## Method index

[sign](#sign) · [verify](#verify) · [encrypt](#encrypt) · [decrypt](#decrypt) · [digest](#digest) · [queryKey](#queryKey)

<a id="sign"></a>

## sign

`String sign(SecuritySignRequest request)`

### Request

| Field | Type | Requirement |
| --- | --- | --- |
| merchantId | String | Nonblank business merchant ID, without the platform prefix |
| algorithm | SecuritySignAlgorithm | Required; must match the institution protocol |
| content | String | Exact content; do not trim or reorder. RSA2Base64 uses Base64 input |

### Response

| Type | Meaning |
| --- | --- |
| String | Native IBCM signature. HMAC_SHA256 returns lowercase HEX; the other supported signing algorithms return Base64. The Adapter adds any institution prefix |

<a id="verify"></a>

## verify

`boolean verify(SecurityVerifyRequest request)`

### Request

| Field | Type | Requirement |
| --- | --- | --- |
| merchantId | String | Nonblank business merchant ID |
| algorithm | SecuritySignAlgorithm | Must match the signing protocol |
| content | String | Original content to verify |
| signature | String | Algorithm-native signature without institution-specific prefixes |

### Response

| Type | Meaning |
| --- | --- |
| boolean | false must block processing. IBCM internal calculation errors can also return false; platform parameter or key-query failures may throw exceptions |

<a id="encrypt"></a>

## encrypt

`String encrypt(SecurityEncryptRequest request)`

### Request

| Field | Type | Requirement |
| --- | --- | --- |
| merchantId | String | Nonblank business merchant ID |
| cipherType | SecurityCipherType | Must match the symmetric/asymmetric classification of the algorithm |
| algorithm | SecurityCipherAlgorithm | See the 12 algorithms below |
| content | String | Non-null; CMS_BASE64_DESded, SM4, and SM2_CITICSH require Base64 input. Other algorithms follow their native protocols |
| parameters | SecurityCipherParameters | Explicit parameters required by the algorithm; must not contain key material or a private-key passphrase |

### Response

| Type | Meaning |
| --- | --- |
| String | Native IBCM ciphertext. PGP may use armored text or Base64 of a binary packet. Block-cipher IVs are not automatically prepended to ciphertext |

<a id="decrypt"></a>

## decrypt

`String decrypt(SecurityDecryptRequest request)`

### Request

| Field | Type | Requirement |
| --- | --- | --- |
| merchantId | String | Nonblank business merchant ID |
| cipherType | SecurityCipherType | Must match the algorithm |
| algorithm | SecurityCipherAlgorithm | Must match the institution ciphertext protocol |
| ciphertext | String | Nonblank, usually Base64; armored text when PGP armor=true |
| parameters | SecurityCipherParameters | Supply applicable protocol parameters such as mode, padding, IV, and AAD |

### Response

| Type | Meaning |
| --- | --- |
| String | SM2 and SM2_CITICSH return Base64 plaintext; others return text. Failures throw exceptions; never return the input ciphertext as a successful result |

## SecurityCipherParameters

| Field | Type | Applicability |
| --- | --- | --- |
| mode | SecurityCipherMode | Required for AES, DES, DESede, RSA2, SM4, and SM4_BOCSZ; RSA2 requires ECB |
| padding | SecurityCipherPadding | Required for the algorithms above; GCM/CTR/CFB require NoPadding |
| ivBase64 | String | Required for CBC/CFB/CTR/GCM; Base64-encoded. Omit for ECB. Use a fresh random IV for encryption; never reuse a GCM/CTR nonce with the same key |
| tagBitLength | Integer | Required for GCM: 96/104/112/120/128; omit for other modes |
| aad | String | Optional authentication text for GCM; must match on decryption. Omit for other modes |
| convert2C1C3C2 | Boolean | SM2_CITICSH decryption only; null/true uses ASN.1 conversion, while false selects the IBCM hex-representation path |
| pgp | SecurityPgpParameters | Required for PGP only; omit for other algorithms |

Parameter identifiers do not guarantee support for every algorithm/mode/padding combination. Validate against the target runtime and institution test vectors.
CBC/CFB/CTR IV length equals the cipher block length. RSA does not use an IV. Never use fixed test nonces in real requests.

## SecurityPgpParameters

| Field | Type | Description |
| --- | --- | --- |
| encryptAlgorithm | SecurityPgpCipherAlgorithm | Required for PGP encryption; omit for decryption, which reads it from the packet |
| signAndVerify | boolean | Sign while encrypting and verify while decrypting; not a standalone signature mode |
| signHashAlgorithm | SecurityPgpHashAlgorithm | Required only for encryption with signAndVerify=true |
| armor | boolean | true uses armored text; false uses Base64 of a PGP binary packet |

The platform queries keys and private-key passphrases; do not pass them in parameters.

<a id="digest"></a>

## digest

`String digest(SecurityDigestRequest request)`

### Request

| Field | Type | Description |
| --- | --- | --- |
| algorithm | SecurityDigestAlgorithm | SHA256/SHA384/SHA512 |
| content | String | UTF-8 text; null is treated as an empty string |
| encoding | SecurityEncoding | HEX or BASE64; defaults to BASE64 |

### Response

| Type | Meaning |
| --- | --- |
| String | Digest text; no key query. A digest is not identity authentication |

<a id="queryKey"></a>

## queryKey

`SecurityKeyMaterial queryKey(SecurityKeyRequest request)`

### Request

| Field | Type | Description |
| --- | --- | --- |
| merchantId | String | Nonblank business merchant ID |
| purpose | SecurityKeyPurpose | SIGN/VERIFY/ENCRYPT/DECRYPT select the usage scenario and material ownership |
| keyAlgorithm | SecurityKeyAlgorithm | Registered key algorithm; not necessarily the signing/encryption computation algorithm |

### Response

Returns one SecurityKeyMaterial, not a list. Throws if no current material is available.

| Field | Type | Description |
| --- | --- | --- |
| value | String | Original stored format, for in-process computation only; do not log, persist, or transmit it |
| keyVersion | String | Version actually selected; not an input |
| keyType | SecurityKeyType | PUBLIC_KEY/PRIVATE_KEY/SYMMETRIC_KEY |
| keyAlgorithm | SecurityKeyAlgorithm | Classification used for this query |

For asymmetric keys, SIGN/DECRYPT query the platform private key and VERIFY/ENCRYPT query the institution public key. Symmetric algorithms query shared material.
Digest registrations query SALT. USER_AUTH/APP_AUTH and similar algorithms query SecretKey, not username/password pairs; no AUTH scene is introduced.
value has no accompanying codeType. Confirm the storage encoding in advance; do not infer it from the algorithm name.
ECC_P256 and ECC_P384 are compatibility aliases for ECC_P256v1 and ECC_P384v1, respectively.

## Algorithms and parameters

There are 16 signing algorithms, 12 encryption algorithms, and 46 key-query algorithms plus two compatibility aliases. These are independent sets.
SHA-1, MD5, DES, and similar algorithms are provided only for compatibility with existing institution protocols, not as defaults for new integrations.

## SecurityCipherAlgorithm

`AES`, `DES`, `DESede`, `RSA2`, `PGP`, `CMS_DESded`, `CMS_BASE64_DESded`, `SM2`, `SM2_CBMC`, `SM2_CITICSH`, `SM4`, `SM4_BOCSZ`.

## SecurityCipherType

`SYMMETRIC`, `ASYMMETRIC`.

## SecurityDigestAlgorithm

`SHA256`, `SHA384`, `SHA512`.

## SecurityEncoding

`BASE64`, `HEX`.

## SecurityKeyAlgorithm

`RSA2`, `HMAC_SHA256`, `HMAC_SHA384`, `HMAC_SHA512`, `ECC_P256`, `ECC_P384`, `AES`, `MD5`, `MD2`, `SHA_1_DIGEST`, `SHA256`, `SHA224`, `SHA384`, `SHA512`, `HMAC_SHA1_BASE64`, `HMAC_SHA224_BASE64`, `HMAC_SHA256_BASE64`, `HMAC_SHA384_BASE64`, `HMAC_SHA512_BASE64`, `HMAC_MD5_BASE64`, `DESede`, `DES`, `HTTP_BASIC`, `COMMON`, `SM4_SHRCCN`, `SM3`, `SM4`, `SM4_BOCSZ`, `USER_AUTH`, `APP_AUTH`, `ONE_KEY_AUTH`, `SM2`, `SM2_CBMC`, `SM2_PABC`, `SM2_CBM`, `SM2_CITICSH`, `SM2_BOCSZ`, `SM2_BJCN`, `SM2_SPDBANK`, `SM2_SHRCCN`, `ECC_P256v1`, `RSAMD5`, `ECC_P512v1`, `SECP256K1`, `SECP256R1`, `RSA1`, `CMS_BASE64_DESded`, `ED25519`.

## SecurityKeyPurpose

`SIGN`, `VERIFY`, `ENCRYPT`, `DECRYPT`.

## SecurityKeyType

`PUBLIC_KEY`, `PRIVATE_KEY`, `SYMMETRIC_KEY`.

## SecuritySignAlgorithm

`RSA1`, `RSA1_HEX`, `RSA2`, `RSA512`, `RSAMD5`, `HMAC_SHA256`, `HMAC_SHA1_BASE64`, `HMAC_SHA224_BASE64`, `HMAC_SHA256_BASE64`, `HMAC_SHA384_BASE64`, `HMAC_SHA512_BASE64`, `HMAC_MD5_BASE64`, `ECC_P256v1`, `RSA2Base64`, `SHA256withRSA_PSS`, `SECP256K1`.

## SecurityCipherMode

`CBC`, `ECB`, `GCM`, `CFB`, `CTR`.

## SecurityCipherPadding

`PKCS5Padding`, `PKCS7Padding`, `PKCS1Padding`, `NoPadding`, `ISO10126Padding`, `SSL3Padding`, `OAEPPadding`, `OAEPWithSHA_256AndMGF1Padding`, `OAEPWithSHA_1AndMGF1Padding`.

## SecurityPgpCipherAlgorithm

`CAST5`, `AES_128`, `AES_192`, `AES_256`, `IDEA`, `TRIPLE_DES`, `BLOWFISH`, `SAFER`, `DES`, `TWOFISH`, `CAMELLIA_128`, `CAMELLIA_192`, `CAMELLIA_256`.

## SecurityPgpHashAlgorithm

`MD5`, `SHA1`, `SHA256`, `SHA384`, `SHA512`, `SHA224`, `RIPEMD160`, `DOUBLE_SHA`, `MD2`, `TIGER_192`, `HAVAL_5_160`.


## Minimal example

```java
String merchantId = ChannelRequestContext.current().getMerchantId();
String signature = securityService.sign(SecuritySignRequest.builder()
        .merchantId(merchantId)
        .algorithm(SecuritySignAlgorithm.HMAC_SHA256_BASE64)
        .content(canonicalText)
        .build());
boolean verified = securityService.verify(SecurityVerifyRequest.builder()
        .merchantId(merchantId)
        .algorithm(SecuritySignAlgorithm.HMAC_SHA256_BASE64)
        .content(canonicalText)
        .signature(signature)
        .build());
if (!verified) {
    throw new SecurityException("Channel signature verification failed");
}
```

Implement content preparation and signature placement according to the institution protocol. Merchant identity comes from the platform request context. This is not a complete security integration for any institution.

Source: SDK api/security package; platform DefaultPlatformChannelSecurityService, IbcmSignatureOperations, and LibraryEncryptionOperations.
