package exchange.exc;

public class InsufficientAssets extends SEException {
    public InsufficientAssets(String assets, long requestedVolume) {
        super("Insufficient funds of " + assets + ", requested volume: " + requestedVolume);
    }
}
