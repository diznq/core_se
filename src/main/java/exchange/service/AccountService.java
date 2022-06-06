package exchange.service;

import exchange.model.Account;
import exchange.model.Asset;
import exchange.repo.AccountRepository;
import exchange.repo.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AccountService {
    @Autowired
    AccountRepository accountRepository;

    @Autowired
    AssetRepository assetRepository;

    public Optional<Asset> getAssetByName(Account account, String name) {
        Asset.AssetKey key = new Asset.AssetKey();
        key.setAssetId(name);
        key.setAccountId(account.getId());
        return assetRepository.findById(key);
    }

    public Optional<Account> getAccountByName(String name) {
        Optional<Account> result = accountRepository.findById(name);
        if (result.isEmpty()) {
            Account account = new Account();
            account.setId(name);
            account = accountRepository.save(account);
            return Optional.of(account);
        } else {
            return result;
        }
    }

    @Transactional
    public Asset manipulate(Account account, String assetId, long volume, long reserve) {
        Optional<Asset> result = getAssetByName(account, assetId);
        if (result.isEmpty()) {
            Asset asset = new Asset();
            Asset.AssetKey key = new Asset.AssetKey();
            key.setAccountId(account.getId());
            key.setAssetId(assetId);
            asset.setId(key);
            asset.setAmount(volume);
            asset.setReserved(reserve);
            asset = assetRepository.save(asset);
            return asset;
        } else {
            Asset asset = result.get();
            asset.setAmount(asset.getAmount() + volume);
            asset.setReserved(asset.getReserved() + reserve);
            return assetRepository.save(asset);
        }
    }

    public Asset reserve(Account account, String assetId, long volume) {
        return manipulate(account, assetId, 0L, volume);
    }

    public Asset transfer(Account account, String assetId, long volume) {
        return manipulate(account, assetId, volume, 0L);
    }

    public Mono<Boolean> hasEnough(Account account, String assetId, long required) {
        Optional<Asset> result = getAssetByName(account, assetId);
        if (result.isEmpty()) return Mono.just(false);
        Asset asset = result.get();
        return Mono.just(asset.getAmount() - asset.getReserved() >= required);
    }

    public Map<String, Long> getPublicAssets(Account account) {
        Map<String, Long> result = new HashMap<>();
        for (Asset asset : account.getAssets()) {
            result.put(asset.getId().getAssetId(), asset.getAmount() - asset.getReserved());
        }
        return result;
    }
}
