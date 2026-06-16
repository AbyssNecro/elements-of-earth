package com.example.eoe;

import javafx.scene.Node;
import javafx.scene.layout.Region;

/**
 * 子ノードを指定アスペクト比（height = width * ratio）でレターボックス配置するレイアウト。
 * 状態を持たない純粋な View 部品。
 */
final class AspectRatioPane extends Region {
    private final Node content;
    private final double ratio;

    AspectRatioPane(Node content, double ratio) {
        this.content = content;
        this.ratio = ratio;
        getChildren().add(content);
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        double expectedHeight = width * ratio;
        if (expectedHeight <= height) {
            content.resizeRelocate(0, (height - expectedHeight) / 2, width, expectedHeight);
        } else {
            double expectedWidth = height / ratio;
            content.resizeRelocate((width - expectedWidth) / 2, 0, expectedWidth, height);
        }
    }

    @Override
    protected double computePrefWidth(double height) {
        return height / ratio;
    }

    @Override
    protected double computePrefHeight(double width) {
        return width * ratio;
    }

    @Override
    protected double computeMinWidth(double height) {
        return 0;
    }

    @Override
    protected double computeMinHeight(double width) {
        return 0;
    }
}
