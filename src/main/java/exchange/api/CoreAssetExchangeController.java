package exchange.api;

import exchange.core.*;
import exchange.view.AccountView;
import exchange.view.OrderBookView;
import exchange.vm.Compiler;
import exchange.vm.Script;
import exchange.vm.ScriptResult;
import exchange.vm.VM;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;


@RestController
public class CoreAssetExchangeController {
    @Autowired
    AssetRepository assetRepository;

    @PostMapping("/book/{base}/{quote}")
    public OrderBookView createOrderBook(@PathVariable("base") String base, @PathVariable("quote") String quote) {
        return new OrderBookView(assetRepository.getOrderBook(base, quote), assetRepository);
    }

    @GetMapping("/book/{base}/{quote}")
    public OrderBookView getOrderBook(@PathVariable("base") String base, @PathVariable("quote") String quote) {
        return new OrderBookView(assetRepository.getOrderBook(base, quote), assetRepository);
    }

    @PostMapping("/account/{account}")
    public AccountView createAccount(@PathVariable("account") String name) {
        return new AccountView(assetRepository.getAccount(name), assetRepository);
    }

    @GetMapping("/account/{account}")
    public AccountView getAccount(@PathVariable("account") String name) {
        return new AccountView(assetRepository.getAccount(name), assetRepository);
    }

    @PostMapping("/account/{account}/deposit/{asset}")
    public AccountView depositAssets(@PathVariable("account") String name, @PathVariable("asset") String asset,
                                     @RequestParam("volume") Long volume) {
        Account account = assetRepository.getAccount(name);
        assetRepository.transferTo(account, asset, volume);
        return new AccountView(account, assetRepository);
    }

    @PostMapping("/book/{base}/{quote}/bid")
    public Order placeBid(@PathVariable("base") String base,
                          @PathVariable("quote") String quote,
                          @RequestParam("account") String name,
                          @RequestParam("price") Long price,
                          @RequestParam("volume") Long volume,
                          @RequestParam(value = "ttl", defaultValue = "0") Long ttl
    ) {
        Account account = assetRepository.getAccount(name);
        OrderBook ob = assetRepository.getOrderBook(base, quote);
        return assetRepository.placeBid(ob, new Order(price, volume, account.getId()), ttl);
    }

    @PostMapping("/book/{base}/{quote}/ask")
    public Order placeAsk(@PathVariable("base") String base,
                          @PathVariable("quote") String quote,
                          @RequestParam("account") String name,
                          @RequestParam("price") Long price,
                          @RequestParam("volume") Long volume,
                          @RequestParam(value = "ttl", defaultValue = "0") Long ttl
    ) {
        Account account = assetRepository.getAccount(name);
        OrderBook ob = assetRepository.getOrderBook(base, quote);
        return assetRepository.placeAsk(ob, new Order(price, volume, account.getId()), ttl);
    }

    @PostMapping("/tick")
    public Long tick(
            @RequestParam(value = "steps", defaultValue = "1") Integer steps
    ) {
        long matches = 0L;
        for (int i = 0; i < steps; i++) {
            matches += assetRepository.advance();
        }
        return matches;
    }

    @PostMapping("/compile")
    public Script compile(@RequestBody String script) {
        return Compiler.compile(script);
    }

    @PostMapping("/exec")
    public ScriptResult exec(@RequestBody String script) {
        return VM.exec(Compiler.compile(script)).result();
    }

    @GetMapping(path = "/book/{base}/{quote}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent> subscribeToStream(@PathVariable("base") String base,
                                      @PathVariable("quote") String quote){
        OrderBook ob = assetRepository.getOrderBook(base, quote);
        return ob.getSink().asFlux().map(tx -> ServerSentEvent.builder(tx).build());
    }

}
