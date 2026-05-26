package me.shimmy.tlntd;

public class StepData {
    public int     ticks   = 20;
    public int     hold    = 1;
    public String  key     = "S";
    public boolean enabled = true;

    public StepData() {}

    public StepData(int ticks, int hold, String key, boolean enabled) {
        this.ticks   = ticks;
        this.hold    = hold;
        this.key     = key;
        this.enabled = enabled;
    }

    public static StepData defaults() {
        return new StepData(20, 1, "S", true);
    }
}
