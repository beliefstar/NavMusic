package com.zx.navmusic.service.strategy;

public interface PlayModeStrategy {
    int LINEAR = 0;
    int RANDOM = 1;
    int SINGLE = 2;

    int getType();
    int getCurPos();
    void resetPos(int position);
    int previous();
    int next();
    int peekPrevious();
    int peekNext();
}
