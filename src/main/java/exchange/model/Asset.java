package exchange.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Entity
public class Asset {
    long amount;
    long reserved;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    @JsonIgnore
    Account account;
    @EmbeddedId
    private AssetKey id;

    public AssetKey getId() {
        return id;
    }

    public void setId(AssetKey id) {
        this.id = id;
    }

    public Account getAccount() {
        return account;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public long getReserved() {
        return reserved;
    }

    public void setReserved(long reserved) {
        this.reserved = reserved;
    }

    @Embeddable
    @Data
    public static class AssetKey implements Serializable {
        @Column(name = "account_id", nullable = false, updatable = false)
        public String accountId;
        @Column(nullable = false, updatable = false)
        public String assetId;

        public AssetKey() {

        }

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getAssetId() {
            return assetId;
        }

        public void setAssetId(String assetId) {
            this.assetId = assetId;
        }
    }
}
