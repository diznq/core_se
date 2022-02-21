package exchange.view;

import exchange.core.Account;
import exchange.core.AssetRepository;

import java.util.HashMap;
import java.util.Map;

public class AccountView {
    public long id;
    public String extra;
    public Map<String, Long> assets = new HashMap<>();

    public AccountView(Account account, AssetRepository repository) {
        id = account.getId();
        extra = account.getExtra();
        for (Map.Entry<Long, Long> entry : account.getPublicAssets().entrySet()) {
            assets.put(repository.getSymbol(entry.getKey()), entry.getValue());
        }
    }
}
