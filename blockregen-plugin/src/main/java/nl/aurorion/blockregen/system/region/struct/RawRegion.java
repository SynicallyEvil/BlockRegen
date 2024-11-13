package nl.aurorion.blockregen.system.region.struct;

import lombok.Getter;
import lombok.Setter;
import nl.aurorion.blockregen.util.LocationUtil;
import org.bukkit.Location;

import java.util.List;

public class RawRegion {

    @Getter
    private final String name;
    @Getter
    private final String min;
    @Getter
    private final String max;

    @Getter
    private final List<String> blockPresets;

    @Getter
    private final boolean all;

    @Getter
    private final int priority;

    @Getter
    @Setter
    private boolean reattempt = false;

    public RawRegion(String name, String min, String max, List<String> blockPresets, boolean all, int priority) {
        this.name = name;
        this.min = min;
        this.max = max;
        this.blockPresets = blockPresets;
        this.all = all;
        this.priority = priority;
    }

    public RegenerationRegion build() {
        Location min = LocationUtil.locationFromString(this.min);
        Location max = LocationUtil.locationFromString(this.max);

        if (min == null || max == null) {
            return null;
        }

        Location actualMin = new Location(min.getWorld(), Double.min(min.getX(), max.getX()), Double.min(min.getY(), max.getY()), Double.min(min.getZ(), max.getZ()));
        Location actualMax = new Location(min.getWorld(), Double.max(min.getX(), max.getX()), Double.max(min.getY(), max.getY()), Double.max(min.getZ(), max.getZ()));

        RegenerationRegion region = new RegenerationRegion(name, actualMin, actualMax);
        region.setPriority(priority);
        region.setAll(all);
        return region;
    }
}
