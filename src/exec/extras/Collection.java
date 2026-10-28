package exec.extras;

import types.datatypes.Vector3;
import types.things.Thing;

import java.util.Arrays;

public class Collection extends Thing{

    public String name;
    public Thing[] things;

    public Collection(String name) {
        this.name = name;
        things = new Thing[10];

        this.localCoordinates = new Vector3(0, 0, 0);
        this.localScale = new Vector3(1, 1, 1);
        this.localRotation = new Vector3(0, 0, 0);
    }

    public void storeThing(Thing thing) {

        int index = findNextThingIndex();

        if (index == -1) {
            growThings();
            index = findNextThingIndex();
        }

        things[index] = thing;

    }

    private int findNextThingIndex() {
        for (int i = 0; i < things.length; i++) {
            if (things[i] == null) {
                return i;
            }
        }

        return -1;
    }
    private void growThings() {
        things = Arrays.copyOf(
                things,
                Math.max(1, things.length * 2)
        );
    }
}
