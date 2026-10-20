package exec.extras;

import types.datatypes.Vector3;
import types.things.Thing;

import java.util.Arrays;

public class Collection {

    public String name;
    public Thing[] things;

    public Vector3 coordinates;
    public Vector3 scale;
    public Vector3 rotation;

    public Collection(String name) {
        this.name = name;
        things = new Thing[10];

        this.coordinates = new Vector3(0, 0, 0);
        this.scale = new Vector3(1, 1, 1);
        this.rotation = new Vector3(0, 0, 0);
    }

    public Collection(String name, Thing[] things, Vector3 coordinates, Vector3 scale, Vector3 rotation) {
        this.name = name;
        this.things = things;
        this.coordinates = coordinates;
        this.scale = scale;
        this.rotation = rotation;
    }

    public int storeThing(Thing thing) {

        int index = findNextThingIndex();

        if (index == -1) {
            growThings();
            index = findNextThingIndex();
        }

        things[index] = thing;

        return index;
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
