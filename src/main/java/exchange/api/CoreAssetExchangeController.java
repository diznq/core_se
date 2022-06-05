package exchange.api;

import exchange.core.AssetRepository;
import exchange.core.Order;
import exchange.core.OrderSide;
import exchange.core.Tx;
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
    AssetRepository assetRepository;

    @PostMapping("/book/{base}/{quote}")
    public Mono<OrderBookView> createOrderBook(@PathVariable("base") String base, @PathVariable("quote") String quote) {
        return assetRepository
                .getOrderBook(base, quote)
                .map(ob -> new OrderBookView(ob, assetRepository));
    }

    @GetMapping("/book/{base}/{quote}")
    public Mono<OrderBookView> getOrderBook(@PathVariable("base") String base, @PathVariable("quote") String quote) {
        return assetRepository
                .getOrderBook(base, quote)
                .map(ob -> new OrderBookView(ob, assetRepository));
    }

    @GetMapping(path = "/book/{base}/{quote}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Tx>> subscribeToStream(@PathVariable("base") String base,
                                                       @PathVariable("quote") String quote) {
        return assetRepository
                .getOrderBook(base, quote)
                .flatMapMany(ob -> ob.getSink().asFlux().map(tx -> ServerSentEvent.builder(tx).build()));
    }

    @PostMapping("/account/{account}")
    public Mono<AccountView> createAccount(@PathVariable("account") String name) {
        return assetRepository
                .getAccount(name)
                .map(account -> new AccountView(account, assetRepository));
    }

    @GetMapping("/account/{account}")
    public Mono<AccountView> getAccount(@PathVariable("account") String name) {
        return assetRepository
                .getAccount(name)
                .map(account -> new AccountView(account, assetRepository));
    }

    @PostMapping("/account/{account}/deposit/{asset}")
    public Mono<AccountView> depositAssets(@PathVariable("account") String name, @PathVariable("asset") String asset,
                                           @RequestParam("volume") Long volume) {
        return assetRepository
                .getAccount(name)
                .flatMap(account ->
                        assetRepository
                                .transferTo(account, asset, volume, "deposit")
                                .thenReturn(account)
                )
                .map(account -> new AccountView(account, assetRepository));
    }

    @PostMapping("/book/{base}/{quote}/bid")
    public Mono<Order> placeBid(@PathVariable("base") String base,
                                @PathVariable("quote") String quote,
                                @RequestParam("account") String name,
                                @RequestParam("price") Long price,
                                @RequestParam("volume") Long volume
    ) {
        return assetRepository.getAccount(name)
                .zipWith(assetRepository.getOrderBook(base, quote))
                .flatMap(tuple -> assetRepository.placeOrder(
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
        return assetRepository.getAccount(name)
                .zipWith(assetRepository.getOrderBook(base, quote))
                .flatMap(tuple -> assetRepository.placeOrder(
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
                .flatMap(step -> assetRepository.advance())
                .reduce(Long::sum);
    }

    @PostMapping("/exec")
    public ScriptResult exec(@RequestBody String script) {
        return VM.exec(Compiler.compile(script)).result();
    }
}
