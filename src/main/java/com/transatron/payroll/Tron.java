package com.transatron.payroll;

import com.google.protobuf.ByteString;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.bouncycastle.util.encoders.Hex;
import org.tron.trident.proto.Chain;
import org.tron.trident.utils.Base58Check;

import java.util.Arrays;

public abstract class Tron {

    public static final long TRXDecimals = 1000000;
    public static final long USDTDecimals = 1000000;
    public static final double TRXInvDecimals = 1.0/TRXDecimals;
    public static final double USDTInvDecimals = 1.0/USDTDecimals;
    public static final String USDTContractAddress = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t";
    public enum ContractType {
        UndefinedType(-1),
        AccountCreateContract(0),
        TransferContract(1),
        TransferAssetContract(2),
        VoteAssetContract(3),
        VoteWitnessContract(4),
        WitnessCreateContract(5),
        AssetIssueContract(6),
        WitnessUpdateContract(8),
        ParticipateAssetIssueContract(9),
        AccountUpdateContract(10),
        FreezeBalanceContract(11),
        UnfreezeBalanceContract(12),
        WithdrawBalanceContract(13),
        UnfreezeAssetContract(14),
        UpdateAssetContract(15),
        ProposalCreateContract(16),
        ProposalApproveContract(17),
        ProposalDeleteContract(18),
        SetAccountIdContract(19),
        CustomContract(20),
        CreateSmartContract(30),
        TriggerSmartContract(31),
        GetContract(32),
        UpdateSettingContract(33),
        ExchangeCreateContract(41),
        ExchangeInjectContract(42),
        ExchangeWithdrawContract(43),
        ExchangeTransactionContract(44),
        UpdateEnergyLimitContract(45),
        AccountPermissionUpdateContract(46),
        ClearABIContract(48),
        UpdateBrokerageContract(49),
        ShieldedTransferContract(51),
        MarketSellAssetContract(52),
        MarketCancelOrderContract(53),
        FreezeBalanceV2Contract(54),
        UnfreezeBalanceV2Contract(55),
        WithdrawExpireUnfreezeContract(56),
        DelegateResourceContract(57),
        UnDelegateResourceContract(58),
        CancelAllUnfreezeV2Contract(59);

        private int num;

        ContractType(int num) { this.num = num; }

        public static ContractType getContractTypeByNum(int num) {
            for(ContractType type : ContractType.values()){
                if(type.getNum() == num)
                    return type;
            }
            return ContractType.UndefinedType;
        }
        public int getNum() {
            return num;
        }
    }



    public static byte[] toHex(String base58Address) {
        byte[] decodedAddress = Base58Check.base58ToBytes(base58Address);
        return Arrays.copyOfRange(decodedAddress, 1,21);
    }

    public static String toBase58(byte[] address) {
        byte[] addressWithPrefix = new byte[21];
        addressWithPrefix[0] = 0x41;
        System.arraycopy(address, 0, addressWithPrefix, 1, 20);
        return Base58Check.bytesToBase58(addressWithPrefix);
    }

    public static String tronHexToBase58(ByteString address) {
        if(address.toByteArray().length != 21)
            throw new IllegalArgumentException("Address length must be 21 bytes");
        if(address.toByteArray()[0] != 65)
            throw new IllegalArgumentException("Hex address header does not match Tron header");
        byte[] keyBytes = new byte[20];
        //truncate 1 byte header "0x41"
        System.arraycopy(address.toByteArray(), 1, keyBytes, 0, 20);
        return Tron.toBase58(keyBytes);
    }

    public static String tronHexToBase58(String address41) {
        byte[] data = Hex.decode(address41);
        if(data.length != 21)
            throw new IllegalArgumentException("Address length must be 21 bytes");
        if(data[0] != 65)
            throw new IllegalArgumentException("Hex address header does not match Tron header");
        byte[] keyBytes = new byte[20];
        //truncate 1 byte header "0x41"
        System.arraycopy(data, 1, keyBytes, 0, 20);
        return Tron.toBase58(keyBytes);
    }

    public static String tronBytesToPrefixedHex(byte[] address){
        if(address.length != 20)
            throw new IllegalArgumentException("Address length must be 20 bytes");
        byte[] addressWithPrefix = new byte[21];
        addressWithPrefix[0] = 0x41;
        System.arraycopy(address, 0, addressWithPrefix, 1, 20);
        return Hex.toHexString(addressWithPrefix);
    }

    public static byte[] hexStringToBytes(String hexString) {
        if(hexString.startsWith("0x"))
            hexString = hexString.substring(2);
        return Hex.decode(hexString);
    }

    public static String toHexString(byte[] data) {
        return "0x" + Hex.toHexString(data);
    }

    public static String getTransactionHash(Chain.Transaction tx){
        return ByteString.copyFrom(Hex.encode(calculateTransactionHash(tx))).toStringUtf8();
    }
    private static byte[] calculateTransactionHash (Chain.Transaction txn) {
        SHA256.Digest digest = new SHA256.Digest();
        digest.update(txn.getRawData().toByteArray());
        byte[] txid = digest.digest();

        return txid;
    }

    public static boolean isTransactionSmartContractCallSucceeded(Chain.Transaction tx){
        boolean success = false;
        if(tx.getRetList().size() >0){
            success = true;
            for(Chain.Transaction.Result ret : tx.getRetList()){
                success = success && (ret.getContractRet() == Chain.Transaction.Result.contractResult.SUCCESS);
            }
        }
        return success;
    }

}
