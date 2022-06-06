package exchange.api;

import exchange.constant.OrderSide;
import exchange.core.AssetManager;
import exchange.model.Order;
import exchange.model.Tx;
import exchange.view.AccountView;
import exchange.view.OrderBookView;
import exchange.vm.Compiler;
import exchange.vm.ScriptResult;
import exchange.vm.VM;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
public class CoreAssetExchangeController {
    @Autowired
    AssetManager assetManager;

    @PostMapping("/book/{base}/{quote}")
    public Mono<OrderBookView> createOrderBook(@PathVariable("base") String base, @PathVariable("quote") String quote) {
        return assetManager
                .getOrderBook(base, quote)
                .map(ob -> new OrderBookView(ob, assetManager));
    }

    @GetMapping("/book/{base}/{quote}")
    public Mono<OrderBookView> getOrderBook(@PathVariable("base") String base, @PathVariable("quote") String quote) {
        return assetManager
                .getOrderBook(base, quote)
                .map(ob -> new OrderBookView(ob, assetManager));
    }

    @GetMapping(path = "/book/{base}/{quote}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Tx>> subscribeToStream(@PathVariable("base") String base,
                                                       @PathVariable("quote") String quote) {
        return assetManager
                .getOrderBook(base, quote)
                .flatMapMany(ob -> ob.getSink().asFlux().map(tx -> ServerSentEvent.builder(tx).build()));
    }

    @PostMapping("/account/{account}")
    public Mono<AccountView> createAccount(@PathVariable("account") String name) {
        return assetManager
                .getAccount(name)
                .map(account -> new AccountView(account, assetManager));
    }

    @GetMapping("/account/{account}")
    public Mono<AccountView> getAccount(@PathVariable("account") String name) {
        return assetManager
                .getAccount(name)
                .map(account -> new AccountView(account, assetManager));
    }

    @PostMapping("/account/{account}/deposit/{asset}")
    public Mono<AccountView> depositAssets(@PathVariable("account") String name, @PathVariable("asset") String asset,
                                           @RequestParam("volume") Long volume) {
        return assetManager
                .getAccount(name)
                .map(account -> {
                    assetManager.transferTo(account, asset, volume, "deposit");
                    return new AccountView(account, assetManager);
                });
    }

    @PostMapping("/book/{base}/{quote}/bid")
    public Mono<Order> placeBid(@PathVariable("base") String base,
                                @PathVariable("quote") String quote,
                                @RequestParam("account") String name,
                                @RequestParam("price") Long price,
                                @RequestParam("volume") Long volume
    ) {
        return assetManager.getAccount(name)
                .zipWith(assetManager.getOrderBook(base, quote))
                .flatMap(tuple -> assetManager.placeOrder(
                        tuple.getT2(),
                        new Order(price, volume, tuple.getT1()),
                        OrderSide.BID
                ));
    }

    @PostMapping("/book/{base}/{quote}/ask")
    public Mono<Order> placeAsk(@PathVariable("base") String base,
                                @PathVariable("quote") String quote,
                                @RequestParam("account") String name,
                                @RequestParam("price") Long price,
                                @RequestParam("volume") Long volume
    ) {
        return assetManager.getAccount(name)
                .zipWith(assetManager.getOrderBook(base, quote))
                .flatMap(tuple -> assetManager.placeOrder(
                        tuple.getT2(),
                        new Order(price, volume, tuple.getT1()),
                        OrderSide.ASK
                ));
    }

    @PostMapping("/tick")
    public Mono<Long> tick(
            @RequestParam(value = "steps", defaultValue = "1") Integer steps
    ) {
        return Flux.range(0, steps)
                .flatMap(step -> assetManager.advance())
                .reduce(Long::sum);
    }

    @PostMapping("/exec")
    public ScriptResult exec(@RequestBody String script) {
        return VM.exec(Compiler.compile(script)).result();
    }
}
