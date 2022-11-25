package io.mo.replace;

public interface Variable {
    String getName();
    void init();
    String nextValue();
    String getExpress();
}
