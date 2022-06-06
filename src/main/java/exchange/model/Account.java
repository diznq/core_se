package exchange.model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Account {
    @Id
    @Column(name = "account_id")
    public String id;
    public String password;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "account_id")
    Set<Asset> assets = new HashSet<>();

    public Account() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<Asset> getAssets() {
        return assets;
    }
}
