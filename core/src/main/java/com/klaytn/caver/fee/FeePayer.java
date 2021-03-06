/*
 * Copyright 2019 The caver-java Authors
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.klaytn.caver.fee;

import com.klaytn.caver.crpyto.KlayCredentials;
import com.klaytn.caver.crpyto.KlaySignatureData;
import com.klaytn.caver.utils.KlaySignatureDataUtils;
import com.klaytn.caver.tx.model.KlayRawTransaction;
import com.klaytn.caver.tx.type.AbstractTxType;
import com.klaytn.caver.utils.BytesUtils;
import org.web3j.crypto.Sign;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Numeric;

import java.util.ArrayList;
import java.util.List;

public class FeePayer {

    private KlayCredentials credentials;
    private int chainId;

    public FeePayer(KlayCredentials credentials, int chainId) {
        this.credentials = credentials;
        this.chainId = chainId;
    }

    public KlayRawTransaction sign(AbstractTxType txType) {
        KlaySignatureData feePayerSignatureData = getSignatureData(txType);

        List<RlpType> rlpTypeList = new ArrayList<>(txType.rlpValues());
        rlpTypeList.add(new RlpList(txType.getSenderSignatureData().toRlpList()));
        rlpTypeList.add(RlpString.create(Numeric.hexStringToByteArray(credentials.getAddress())));
        rlpTypeList.add(new RlpList(feePayerSignatureData.toRlpList()));

        byte[] encodedTransaction = RlpEncoder.encode(new RlpList(rlpTypeList));
        byte[] type = {txType.getType().get()};
        byte[] rawTx = BytesUtils.concat(type, encodedTransaction);
        return new KlayRawTransaction(rawTx, feePayerSignatureData);
    }

    public KlaySignatureData getSignatureData(AbstractTxType txType) {
        KlaySignatureData signatureData = KlaySignatureData.createKlaySignatureDataFromChainId(chainId);
        byte[] encodedTransaction = txType.getEncodedTransactionNoSig();

        List<RlpType> rlpTypeList = new ArrayList<>();
        rlpTypeList.add(RlpString.create(encodedTransaction));
        rlpTypeList.add(RlpString.create(Numeric.hexStringToByteArray(credentials.getAddress())));
        rlpTypeList.addAll(signatureData.toRlpList().getValues());
        byte[] encodedTransaction2 = RlpEncoder.encode(new RlpList(rlpTypeList));

        Sign.SignatureData signedSignatureData = Sign.signMessage(encodedTransaction2, credentials.getEcKeyPair());
        return KlaySignatureDataUtils.createEip155KlaySignatureData(signedSignatureData, chainId);
    }
}
