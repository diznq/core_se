package exchange.view;

import exchange.core.AssetManager;
import exchange.model.Account;

import java.util.HashMap;
import java.util.Map;

public class AccountView {
    public String id;
    public Map<String, Long> assets = new HashMap<>();

    public AccountView(Account account, AssetManager repository) {
        id = account.getId();
        assets.putAll(repository.getPublicAssets(account));
    }
}
