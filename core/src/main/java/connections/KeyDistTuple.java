package connections;

import java.security.PublicKey;

public class KeyDistTuple {
    public PublicKey key;
    public int distance;

    public KeyDistTuple(PublicKey k, int d) {
        key = k;
        distance = d;
    }
}
