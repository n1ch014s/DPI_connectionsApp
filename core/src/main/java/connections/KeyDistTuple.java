package connections;

import java.security.PublicKey;

public class KeyDistTuple {
    PublicKey key;
    int distance;

    KeyDistTuple(PublicKey k, int d) {
        key = k;
        distance = d;
    }
}
