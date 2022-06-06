package exchange.view;

import exchange.core.AssetManager;
import exchange.core.OrderBook;
import exchange.model.Order;
import exchange.model.Tx;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class OrderBookView {
    public String left;
    public String right;
    public String name;
    public List<AggregateOrderView> bid;
    public List<AggregateOrderView> ask;
    public long lastPrice;
    public List<Tx> history;
    public AggregateOrderView topBid;
    public AggregateOrderView topAsk;

    public OrderBookView(OrderBook ob, AssetManager repository) {
        left = ob.getLeft();
        right = ob.getRight();
        name = ob.getName();
        bid = toAggregate(ob.getBids()).stream().sorted(AggregateOrderView::inverseCompare).toList();
        ask = toAggregate(ob.getAsks()).stream().sorted(AggregateOrderView::compare).toList();
        Order bestBid = ob.getTopBid();
        Order bestAsk = ob.getTopAsk();
        if (bestBid != null)
            topBid = new AggregateOrderView(bestBid.getPrice(), bestBid.getRealVolume());
        if (bestAsk != null)
            topAsk = new AggregateOrderView(bestAsk.getPrice(), bestAsk.getRealVolume());
        lastPrice = ob.getLastPrice();
        history = ob.getPriceHistory();
    }

    private List<AggregateOrderView> toAggregate(Map<Long, Long> side) {
        List<AggregateOrderView> orders = new LinkedList<>();
        for (Map.Entry<Long, Long> entry : side.entrySet()) {
            orders.add(new AggregateOrderView(entry.getKey(), entry.getValue()));
        }
        return orders;
    }
}
