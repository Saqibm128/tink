// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.google.cloud.crypto.tink.signature;

import static junit.framework.Assert.assertTrue;

import com.google.cloud.crypto.tink.CommonProto.EllipticCurveType;
import com.google.cloud.crypto.tink.CommonProto.HashType;
import com.google.cloud.crypto.tink.EcdsaProto.EcdsaKeyFormat;
import com.google.cloud.crypto.tink.EcdsaProto.EcdsaParams;
import com.google.cloud.crypto.tink.EcdsaProto.EcdsaPublicKey;
import com.google.cloud.crypto.tink.PublicKeyVerify;
import com.google.cloud.crypto.tink.subtle.EcUtil;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import org.junit.Test;

/**
 * Unit tests for EcdsaVerifyKeyManager.
 * TODO(quannguyen): Add more tests.
 */
public class EcdsaVerifyKeyManagerTest {
  @Test
  public void testBasic() throws Exception {
    ECParameterSpec ecParams = EcUtil.getNistP256Params();
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
    keyGen.initialize(ecParams);
    KeyPair keyPair = keyGen.generateKeyPair();
    ECPublicKey pubKey = (ECPublicKey) keyPair.getPublic();
    ECPrivateKey privKey = (ECPrivateKey) keyPair.getPrivate();

    // Sign with JCE's Signature.
    Signature signer = Signature.getInstance("SHA256WithECDSA");
    signer.initSign(privKey);
    String message = "Hello";
    signer.update(message.getBytes("UTF-8"));
    byte[] signature = signer.sign();

    // Create EcdsaPUblicKey protocol buffer
    ECPoint w = pubKey.getW();
    EcdsaParams ecdsaParams = EcdsaParams.newBuilder()
        .setHashType(HashType.SHA256)
        .setCurve(EllipticCurveType.NIST_P256)
        .build();
    EcdsaPublicKey ecdsaPubKey = EcdsaPublicKey.newBuilder()
        .setVersion(0)
        .setParams(ecdsaParams)
        .setX(ByteString.copyFrom(w.getAffineX().toByteArray()))
        .setY(ByteString.copyFrom(w.getAffineY().toByteArray()))
        .build();

    // Verify with PublicKeyVerify gotten from EcdsaVerifyKeyManager.
    EcdsaVerifyKeyManager verifyManager = new EcdsaVerifyKeyManager();
    PublicKeyVerify verifier = verifyManager.getPrimitive(Any.pack(ecdsaPubKey));
    assertTrue(verifier.verify(signature, message.getBytes("UTF-8")));
  }
}
